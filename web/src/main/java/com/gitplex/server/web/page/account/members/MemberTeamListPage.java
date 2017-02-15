package com.gitplex.server.web.page.account.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.gitplex.server.GitPlex;
import com.gitplex.server.entity.Account;
import com.gitplex.server.entity.Team;
import com.gitplex.server.entity.TeamMembership;
import com.gitplex.server.manager.TeamManager;
import com.gitplex.server.manager.TeamMembershipManager;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.web.WebConstants;
import com.gitplex.server.web.component.select2.ChoiceProvider;
import com.gitplex.server.web.component.select2.Response;
import com.gitplex.server.web.component.select2.ResponseFiller;
import com.gitplex.server.web.component.select2.SelectToAddChoice;
import com.gitplex.server.web.component.teamchoice.TeamChoiceResourceReference;
import com.gitplex.server.web.page.account.teams.TeamMemberListPage;

@SuppressWarnings("serial")
public class MemberTeamListPage extends MemberPage {

	private ListView<TeamMembership> teamsView;
	
	private WebMarkupContainer teamsContainer; 
	
	private WebMarkupContainer noTeamsContainer;
	
	private Set<Long> pendingRemovals = new HashSet<>();
	
	public MemberTeamListPage(PageParameters params) {
		super(params);
		
		Preconditions.checkState(getAccount().isOrganization());
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		AjaxLink<Void> confirmRemoveLink;
		add(confirmRemoveLink = new AjaxLink<Void>("confirmRemove") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				TeamMembershipManager teamMembershipManager = GitPlex.getInstance(TeamMembershipManager.class);
				Collection<TeamMembership> memberships = new ArrayList<>();
				for (Long id: pendingRemovals) {
					memberships.add(teamMembershipManager.load(id));
				}
				GitPlex.getInstance(TeamMembershipManager.class).delete(memberships);
				pendingRemovals.clear();
				target.add(this);
				target.add(teamsContainer);
				target.add(noTeamsContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!pendingRemovals.isEmpty());
			}
			
		});
		confirmRemoveLink.setOutputMarkupPlaceholderTag(true);
		
		add(new SelectToAddChoice<Team>("addNew", new ChoiceProvider<Team>() {

			@Override
			public void query(String term, int page, Response<Team> response) {
				List<Team> teams = new ArrayList<>();
				Account user = getMembership().getUser();
				Collection<Team> joinedTeams = new HashSet<>();
				for (TeamMembership membership: user.getJoinedTeams()) {
					joinedTeams.add(membership.getTeam());
				}
				for (Team team: getAccount().getDefinedTeams()) {
					if (team.matches(term) && !joinedTeams.contains(team)) {
						teams.add(team);
					}
				}
				
				Collections.sort(teams);
				
				new ResponseFiller<Team>(response).fill(teams, page, WebConstants.DEFAULT_PAGE_SIZE);
			}

			@Override
			public void toJson(Team choice, JSONWriter writer) throws JSONException {
				writer.key("id").value(choice.getId()).key("name").value(choice.getName());
			}

			@Override
			public Collection<Team> toChoices(Collection<String> ids) {
				List<Team> teams = Lists.newArrayList();
				TeamManager teamManager = GitPlex.getInstance(TeamManager.class);
				for (String each : ids) {
					Long id = Long.valueOf(each);
					teams.add(teamManager.load(id));
				}

				return teams;
			}

		}) {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				getSettings().setPlaceholder("Select team to join...");
				getSettings().setFormatResult("gitplex.server.teamChoiceFormatter.formatResult");
				getSettings().setFormatSelection("gitplex.server.teamChoiceFormatter.formatSelection");
				getSettings().setEscapeMarkup("gitplex.server.teamChoiceFormatter.escapeMarkup");
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canManage(getAccount()));
			}
			
			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);
				
				response.render(JavaScriptHeaderItem.forReference(new TeamChoiceResourceReference()));
			}
			
			@Override
			protected void onSelect(AjaxRequestTarget target, Team selection) {
				TeamMembership membership = new TeamMembership();
				membership.setTeam(selection);
				membership.setUser(getMembership().getUser());
				GitPlex.getInstance(TeamMembershipManager.class).save(membership);
				target.add(teamsContainer);
				target.add(noTeamsContainer);
			}
			
		});
		
		teamsContainer = new WebMarkupContainer("teams") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!teamsView.getModelObject().isEmpty());
			}
			
		};
		teamsContainer.setOutputMarkupPlaceholderTag(true);
		add(teamsContainer);
		
		teamsContainer.add(teamsView = new ListView<TeamMembership>("teams", 
				new LoadableDetachableModel<List<TeamMembership>>() {

			@Override
			protected List<TeamMembership> load() {
				List<TeamMembership> memberships = new ArrayList<>(getMembership().getUser().getJoinedTeams());
				
				memberships.sort((membership1, membership2) 
						-> membership1.getTeam().getName().compareTo(membership2.getTeam().getName()));
				return memberships;
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<TeamMembership> item) {
				Team team = item.getModelObject().getTeam();

				Link<Void> teamLink = new BookmarkablePageLink<Void>("link", 
						TeamMemberListPage.class, TeamMemberListPage.paramsOf(team));
				teamLink.add(new Label("name", team.getName()));
				item.add(teamLink);
				item.add(new AjaxLink<Void>("remove") {

					@Override
					protected void onConfigure() {
						super.onConfigure();

						setVisible(SecurityUtils.canManage(getAccount()) 
								&& !pendingRemovals.contains(item.getModelObject().getId()));
					}

					@Override
					public void onClick(AjaxRequestTarget target) {
						pendingRemovals.add(item.getModelObject().getId());
						target.add(item);
						target.add(confirmRemoveLink);
					}

				});
				item.add(new WebMarkupContainer("pendingRemoval") {

					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(pendingRemovals.contains(item.getModelObject().getId()));
					}
					
				});
				item.add(new AjaxLink<Void>("undoRemove") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						pendingRemovals.remove(item.getModelObject().getId());
						target.add(item);
						target.add(confirmRemoveLink);
					}
					
					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(pendingRemovals.contains(item.getModelObject().getId()));
					}
					
				});
				item.setOutputMarkupId(true);
			}
			
		});

		noTeamsContainer = new WebMarkupContainer("noTeams") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(teamsView.getModelObject().isEmpty());
			}
			
		};
		noTeamsContainer.setOutputMarkupPlaceholderTag(true);
		add(noTeamsContainer);
	}
	
	@Override
	protected boolean isPermitted() {
		return SecurityUtils.canAccess(getAccount());
	}
	
}
