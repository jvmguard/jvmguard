package dev.jvmguard.integration.util

import org.jdom2.Element
import org.jdom2.filter.ElementFilter
import org.jdom2.input.SAXBuilder
import org.jdom2.output.XMLOutputter
import java.io.StringReader

fun List<Element>.all(tagName: String): List<Element> {
    val result = mutableListOf<Element>()
    for (current in this) {
        result.addAll(current.all(tagName))
    }
    return result
}

fun Element.all(tagName: String): List<Element> = if (tagName == "*") children else getChildren(tagName)

fun Element.attr(attrName: String): String? = getAttributeValue(attrName)

fun parseXmlString(xml: String) = requireNotNull(SAXBuilder().build(StringReader(xml)).rootElement)

fun Element.verbose(): String {
    val outputElement = clone()
    for (child in outputElement.children) {
        child.removeContent(ElementFilter())
    }
    return XMLOutputter().outputString(outputElement)
}

fun println(element: Element) {
    println(element.verbose())
}

fun println(elements: List<Element>) {
    for (element in elements) {
        print(element.verbose())
    }
    println()
}

fun Element.findFirst(tagName: String, attrName: String, attrValue: String): Element? {
    for (child in children) {
        if (child is Element) {
            if (child.name == tagName && child.attr(attrName) == attrValue) {
                return child
            } else {
                val grandChild = child.findFirst(tagName, attrName, attrValue)
                if (grandChild != null) {
                    return grandChild
                }
            }
        }
    }
    return null
}
