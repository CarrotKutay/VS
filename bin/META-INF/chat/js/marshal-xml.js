// costants
const DOC_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";


/**
 * Creates a singleton for marshaling application/xml data.
 * The XML format specifics chosen are compatible with the MOXY XML marshaler,
 * especially regarding synthetic type properties and @-prefixed attribute names.
 * Copyright (c) 2013 Sascha Baumeister
 */
class XmlMarshaller extends Object {

	/**
	 * Initializes a new instance.
	 */
	constructor () {
		super();
	}


	/**
	* Recursively marshals the given object into an XML document. The result will contain
	* the given root element, except if the given object is null, an array, or a function.
	* Any primitive typed field (strings, numbers, booleans) is marshaled into an attribute.
	* Any array typed field is marshaled into multiple child elements, while any other
	* Object type is recursively marshaled into a single child element. Note that similarly
	* to JSON.stringify(), joint references to the same child object will be represented
	* as content equal but disjoint XML elements. Also, recursive object references are
	* not supported, and will cause infinite loops.
	* @param {Object} object the object to be marshaled
	* @param {String} rootName root element name for the given object
	* @return {String} the corresponding XML document text
	* @throws {TypeError} if the given object is null, a function or an array
	*/
	marshal (object, rootName) {
		if (object == null || typeof object == "function" || object instanceof Array) throw new TypeError("illegal argument");

		let dom = document.implementation.createDocument(null, rootName);
		marshal.call(this, object, dom.documentElement);

		let serializer = new XMLSerializer();
		return DOC_DECLARATION + serializer.serializeToString(dom);
	}


	/**
	* Recursively unmarshals the given XML text into an object. If the XML root element
	* contains only text, the latter is returned. Otherwise, a generic object is
	* assembled that contains fields named after each element's attributes and child
	* elements. If an element contains multiple child elements sharing the same name,
	* their values are joined into an array. Note that this implies that field values
	* may be undefined (zero occurrences), object types (single occurrence), or array
	* types (multiple occurrences) depending on the XML content.
	* @param {String} xml the XML document text to be unmarshaled
	* @return {Object} the corresponding object
	*/
	unmarshal (xml) {
		let dom = new DOMParser().parseFromString(xml, "text/xml");
		let root = dom.documentElement;
		let object = root.attributes.length === 0 && root.children.length === 0 ? {} : unmarshal.call(this, root);
		object.type = root.nodeName;
		return object;
	}
}


/**
* Private method recursively marshaling the given object's properties into the
* given XML element. Any property with an @-prefixed name is marshaled into an
* attribute. Any array typed field is marshaled into multiple child elements,
* while any other object type is recursively marshaled into a single child element.
* Note that similarly to JSON.stringify(), joint references to the same child
* object will be represented as content equal but disjoint XML elements. Also,
* recursive object references are not supported, and will cause infinite loops.
* Finally, the given element is expected to be owned by a document.
* @param {Object} object the object to be marshaled
* @param {Element} element the resulting DOM element
*/
function marshal (object, element) {
	const type = Object.prototype.toString.call(object);

	if (type == "[object String]" || type == "[object Number]" || type == "[object Boolean]") {
		element.appendChild(element.ownerDocument.createTextNode(object));
	} else {
		for (; object != null; object = Object.getPrototypeOf(object)) {
			for (const key in object) {
				let value = object[key];
				if (value == null || typeof value == "function") continue;

				if (key.startsWith("@")) {
					let node = element.ownerDocument.createAttribute(key.substring(1).trim());
					node.value = value;
					element.setAttributeNode(node);
				} else {
					let values = value instanceof Array ? value : [value];
					for (const object of values) {
						let node = element.ownerDocument.createElement(key);
						element.appendChild(node);
						marshal.call(this, object, node);
					}
				}
			}
		}
	}
}


/**
* Private method recursively unmarshaling the given DOM element into an object. If
* the node is a text node, it's text value is returned. Otherwise a generic object is
* returned that contains fields named after the node's attributes and child elements.
* If a node contains multiple child elements sharing the same name, they are joined
* into an array.
* @param {Element} element the DOM element to be unmarshaled
* @return {Object} the resulting object
*/
function unmarshal (element) {
	if (element.attributes.length === 0 && element.children.length === 0) return element.textContent;

	let object = {};
	for (let attribute of element.attributes) {
		object["@" + attribute.nodeName.trim()] = attribute.nodeValue;
	}

	for (let child of element.children) {
		const key = child.nodeName;
		let value = unmarshal.call(this, child);

		if (key in object) {
			let existingValue = object[key];
			if (existingValue instanceof Array) existingValue.push(value);
			else object[key] = [ existingValue, value ];
		} else {
			object[key] = value;
		}
	}

	return object;
}


export default new XmlMarshaller();