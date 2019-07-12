/**
 * Defines a controller type that is tasked with connecting script
 * actions to the DOM and vice versa, featuring JSON marshaling.
 * Copyright (c) 2008 Sascha Baumeister
 */
class ChatJsonController extends Object {

	/**
	 * initilizes a new instance.
	 */
	constructor () {
		super();
	}

	/**
	* Creates a new server side chat entry using the current values of the HTML
	* "alias" and "content" input fields by issuing an asynchronous REST web-service
	* call (PUT /services/chatEntries). Afterwards, the content of the HTML table
	* "chatEntries" is refreshed using another asynchronous REST web-service call
	* (GET /services/chatEntries).
	*/
	async addChatEntry () {
		let inputs = document.querySelectorAll("input");
		const chatEntry = { alias: inputs[0].value, content: inputs[1].value, timestamp: new Date().getTime() };

		const requestBody = JSON.stringify(chatEntry);
		let response = await fetch("/services/chatEntries", { method: "POST", headers: {"Content-type": "application/json"}, credentials: "omit", body: requestBody });
		if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);

		this.refreshChatEntries();
	}


	/**
	* Updates the content of the HTML "chatEntries" table by issuing an
	* asynchronous REST web-service call (GET /services/chatEntries).
	*/
	async refreshChatEntries () {
		let response = await fetch("/services/chatEntries", { method: "GET", headers: {"Accept": "application/json"}, credentials: "omit" });
		if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
		const chatEntries = await response.json();

		let table = document.querySelector("table");
		while (table.childElementCount > 0) table.removeChild(table.lastElementChild);

		let rowTemplate = document.querySelector("#chat-entry-template");
		for (const chatEntry of chatEntries) {
			let row = rowTemplate.content.cloneNode(true).firstElementChild;
			let outputs = row.querySelectorAll("output")
			outputs[0].value = new Date(parseInt(chatEntry.timestamp)).toLocaleTimeString();
			outputs[1].value = chatEntry.alias;
			outputs[2].value = chatEntry.content;
			table.appendChild(row);
		}
	};
}


/**
* After loading the page, initialize HTML element callbacks and display current
* chat entries. Note that the former cannot be performed during controller creation
* as the HTML elements do not exist yet.
*/
window.addEventListener("load", event => {
	let controller = new ChatJsonController();

	let buttons = document.querySelectorAll("button");
	buttons[0].addEventListener("click", event => controller.addChatEntry());
	buttons[1].addEventListener("click", event => controller.refreshChatEntries());
	buttons[1].click();
});