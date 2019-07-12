import XML from "./marshal-xml.js";


/**
 * Defines a controller type that is tasked with connecting script
 * actions to the DOM and vice versa, featuring XML marshaling.
 * Copyright (c) 2008 Sascha Baumeister
 */
let ChatXmlController = class extends Object {

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
		const inputElements = document.querySelectorAll("input");
		const chatEntry = { alias: inputElements[0].value, content: inputElements[1].value, timestamp: new Date().getTime() };

		const body = XML.marshal(chatEntry, "chatEntry");
		const headers = new Headers({"Content-type": "application/xml"});
		const response = await fetch("/services/chatEntries", { method: "POST", headers: headers, body: body, credentials: "omit" });
		if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);

		this.refreshChatEntries();
	}


	/**
	* Updates the content of the HTML "chatEntries" table by issuing an asynchronous
	* REST web-service call (GET /services/chatEntries).
	*/
	async refreshChatEntries () {
		let response = await fetch("/services/chatEntries", { method: "GET", headers: {"Accept": "application/xml"}, credentials: "omit" });
		if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
		const xml = await response.text();
		const object = XML.unmarshal(xml);
		const chatEntries = object.chatEntry ? (object.chatEntry instanceof Array ? object.chatEntry : [ object.chatEntry ]) : [];

		let table = document.querySelector("table");
		while (table.childElementCount > 0) table.removeChild(table.lastElementChild);

		const rowTemplate = document.querySelector("#chat-entry-template");
		for (const chatEntry of chatEntries) {
			let row = rowTemplate.content.cloneNode(true).firstElementChild;
			let outputs = row.querySelectorAll("output")
			outputs[0].value = new Date(parseInt(chatEntry.timestamp)).toLocaleTimeString();
			outputs[1].value = chatEntry.alias;
			outputs[2].value = chatEntry.content;
			table.appendChild(row);
		}
	}
}


/*
* After loading the page, initialize HTML element callbacks and display current
* chat entries. Note that the former cannot be performed during controller creation
* as the HTML elements do not exist yet.
*/
window.addEventListener("load", event => {
	let controller = new ChatXmlController();

	let buttons = document.querySelectorAll("button");
	buttons[0].addEventListener("click", event => controller.addChatEntry());
	buttons[1].addEventListener("click", event => controller.refreshChatEntries());
	buttons[1].click();
});