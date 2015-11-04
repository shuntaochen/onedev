pmease.commons.modal = {
	init: function(containerId, closeCallback) {
		var $modal = $("#" + containerId + ">.modal");
		$modal.data("closeCallback", closeCallback);
		
		$modal.data("keydown", function(e) {
			if (e.keyCode == 27 && $(".select2-drop:visible").length == 0 && $("body>.floating").length == 0) 
				pmease.commons.modal.close($modal, true);
		});
		
		// use keydown as keypress does not work in chrome/safari
		$(document).on("keydown", $modal.data("keydown"));
		
		$modal.modal({backdrop: "static"});
	}, 
	
	close: function($modal, callCloseCallback) {
		if (callCloseCallback)
			$modal.data("closeCallback")();
		
		$(document).off("keydown", $modal.data("keydown"));
		
		$modal.modal("hide").parent().remove();
	}
	
}