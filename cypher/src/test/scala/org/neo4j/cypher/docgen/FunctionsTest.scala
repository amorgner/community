/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.docgen

import org.junit.Test
import org.junit.Assert._
import org.neo4j.graphdb.Node
import org.neo4j.cypher.ExecutionResult
import collection.mutable.WrappedArray

class FunctionsTest extends DocumentingTestBase {
  def graphDescription = List("A KNOWS B", "A KNOWS C", "B KNOWS D", "C KNOWS D", "B MARRIED E")

  override val properties = Map(
    "A" -> Map("age" -> 38, "eyes" -> "brown"),
    "B" -> Map("age" -> 25, "eyes" -> "blue"),
    "C" -> Map("age" -> 53, "eyes" -> "green"),
    "D" -> Map("age" -> 54, "eyes" -> "brown"),
    "E" -> Map("age" -> 41, "eyes" -> "blue", "array" -> Array("one", "two", "three"))
  )


  def section = "functions"

  val common_arguments = List(
    "iterable" -> "An array property, or an iterable symbol, or an iterable function.",
    "identifier" -> "This is the identifier that can be used from the predicate.",
    "predicate" -> "A predicate that is tested against all items in iterable."
  )

  @Test def all() {
    testThis(
      title = "ALL",
      syntax = "ALL(identifier in iterable WHERE predicate)",
      arguments = common_arguments,
      text = """Tests whether a predicate holds for all element of this iterable collection.""",
      queryText = """start a=node(%A%), b=node(%D%) match p=a-[*1..3]->b where all(x in nodes(p) WHERE x.age > 30) return p""",
      returns = """All nodes in the returned paths will have an `age` property of at least 30.""",
      assertions = (p) => assertEquals(1, p.toSeq.length))
  }

  @Test def any() {
    testThis(
      title = "ANY",
      syntax = "ANY(identifier in iterable WHERE predicate)",
      arguments = common_arguments,
      text = """Tests whether a predicate holds for at least one element of this iterable collection.""",
      queryText = """start a=node(%E%) where any(x in a.array WHERE x = "one") return a""",
      returns = """All nodes in the returned paths has at least one `one` value set in the array property named `array`.""",
      assertions = (p) => assertEquals(List(Map("a"->node("E"))), p.toList))
  }

  @Test def none() {
    testThis(
      title = "NONE",
      syntax = "NONE(identifier in iterable WHERE predicate)",
      arguments = common_arguments,
      text = """Returns true if the predicate holds for no element in the iterable.""",
      queryText = """start n=node(%A%) match p=n-[*1..3]->b where NONE(x in nodes(p) WHERE x.age = 25) return p""",
      returns = """No nodes in the returned paths has a `age` property set to `25`.""",
      assertions = (p) => assertEquals(2, p.toSeq.length))
  }

  @Test def single() {
    testThis(
      title = "SINGLE",
      syntax = "SINGLE(identifier in iterable WHERE predicate)",
      arguments = common_arguments,
      text = """Returns true if the predicate holds for exactly one of the elements in the iterable.""",
      queryText = """start n=node(%A%) match p=n-->b where SINGLE(var in nodes(p) WHERE var.eyes = "blue") return p""",
      returns = """Exactly one node in every returned path will have the `eyes` property set to `"blue"`.""",
      assertions = (p) => assertEquals(1, p.toSeq.length))
  }

  @Test def relationship_type() {
    testThis(
      title = "TYPE",
      syntax = "TYPE( relationship )",
      arguments = List("relationship" -> "A relationship."),
      text = """Returns a string representation of the relationship type.""",
      queryText = """start n=node(%A%) match (n)-[r]->() return type(r)""",
      returns = """The relationship type of `r` is returned by the query.""",
      assertions = (p) => assertEquals("KNOWS", p.columnAs[String]("type(r)").toList.head))
  }

  @Test def length() {
    testThis(
      title = "LENGTH",
      syntax = "LENGTH( iterable )",
      arguments = List("iterable" -> "An iterable, value or function call."),
      text = """To return or filter on the length of a path, use the `LENGTH()` function.""",
      queryText = """start a=node(%A%) match p=a-->b-->c return length(p)""",
      returns = """The length of the path `p` is returned by the query.""",
      assertions = (p) => assertEquals(2, p.columnAs[Int]("length(p)").toList.head))
  }

  @Test def extract() {
    testThis(
      title = "EXTRACT",
      syntax = "EXTRACT( identifier in iterable : expression )",
      arguments = List(
        "iterable" -> "An array property, or an iterable identifier, or an iterable function.",
        "identifier" -> "The closure will have an identifier introduced in it's context. Here you decide which identifier to use.",
        "expression" -> "This expression will run once per value in the iterable, and produces the result iterable."
      ),
      text = """To return a single property, or the value of a function from an iterable of nodes or relationships,
 you can use `EXTRACT`. It will go through all enitities in the iterable, and run an expression, and return the results
 in an iterable with these values. It works like the `map` method in functional languages such as Lisp and Scala.""",
      queryText = """start a=node(%A%), b=node(%B%), c=node(%D%) match p=a-->b-->c return extract(n in nodes(p) : n.age)""",
      returns = """The age property of all nodes in the path are returned.""",
      assertions = (p) => assertEquals(List(Map("extract(n in nodes(p) : n.age)" -> List(38, 25, 54))), p.toList))
  }

  @Test def head() {
    testThis(
      title = "HEAD",
      syntax = "HEAD( expression )",
      arguments = List(
        "expression" -> "This expression should return a collection of some kind."
      ),
      text = "`HEAD` returns the first element in a collection.",
      queryText = """start a=node(%E%) return a.array, head(a.array)""",
      returns = "The first node in the path is returned.",
      assertions = (p) => assertEquals(List("one"), p.columnAs[List[_]]("head(a.array)").toList))
  }

  @Test def last() {
    testThis(
      title = "LAST",
      syntax = "LAST( expression )",
      arguments = List(
        "expression" -> "This expression should return a collection of some kind."
      ),
      text = "`LAST` returns the last element in a collection.",
      queryText = """start a=node(%E%) return a.array, last(a.array)""",
      returns = "The last node in the path is returned.",
      assertions = (p) => assertEquals(List("three"), p.columnAs[List[_]]("last(a.array)").toList))
  }

  @Test def tail() {
    testThis(
      title = "TAIL",
      syntax = "TAIL( expression )",
      arguments = List(
        "expression" -> "This expression should return a collection of some kind."
      ),
      text = "`TAIL` returns all but the first element in a collection.",
      queryText = """start a=node(%E%) return a.array, tail(a.array)""",
      returns = "This returns the property named `array` and all elements of that property except the first one.",
      assertions = (p) => {
        val toList = p.columnAs[WrappedArray[_]]("tail(a.array)").toList.head.toList
        assert(toList === List("two","three"))
      })
  }

  @Test def filter() {
    testThis(
      title = "FILTER",
      syntax = "FILTER(identifier in iterable : predicate)",
      arguments = common_arguments,
      text = "`FILTER` returns all the elements in an iterable that comply to a predicate.",
      queryText = """start a=node(%E%) return a.array, filter(x in a.array : length(x) = 3)""",
      returns = "This returns the property named `array` and a list of values in it, which have the length `3`.",
      assertions = (p) => {
        val array = p.columnAs[WrappedArray[_]]("filter(x in a.array : length(x) = 3)").toList.head
        assert(List("one","two") === array.toList)
      })
  }

  @Test def nodes_in_path() {
    testThis(
      title = "NODES",
      syntax = "NODES( path )",
      arguments = List("path" -> "A path."),
      text = """Returns all nodes in a path.""",
      queryText = """start a=node(%A%), c=node(%E%) match p=a-->b-->c return NODES(p)""",
      returns = """All the nodes in the path `p` are returned by the example query.""",
      assertions = (p) => assert(List(node("A"), node("B"), node("E")) === p.columnAs[List[Node]]("NODES(p)").toList.head)
    )
  }

  @Test def rels_in_path() {
    testThis(
      title = "RELATIONSHIPS",
      syntax = "RELATIONSHIPS( path )",
      arguments = List("path" -> "A path."),
      text = """Returns all relationships in a path.""",
      queryText = """start a=node(%A%), c=node(%E%) match p=a-->b-->c return RELATIONSHIPS(p)""",
      returns = """All the relationships in the path `p` are returned.""",
      assertions = (p) => assert(2 === p.columnAs[List[Node]]("RELATIONSHIPS(p)").toSeq.head.length)
    )
  }

  @Test def id() {
    testThis(
      title = "ID",
      syntax = "ID( property-container )",
      arguments = List("property-container" -> "A node or a relationship."),
      text = """Returns the id of the relationship or node.""",
      queryText = """start a=node(%A%, %B%, %C%) return ID(a)""",
      returns = """This returns the node id for three nodes.""",
      assertions = (p) => assert(Seq(node("A").getId, node("B").getId, node("C").getId) === p.columnAs[Int]("ID(a)").toSeq)
    )
  }

  @Test def coalesce() {
    testThis(
      title = "COALESCE",
      syntax = "COALESCE( expression [, expression]* )",
      arguments = List("expression" -> "The expression that might return null."),
      text = """Returns the first non-+null+ value in the list of expressions passed to it.""",
      queryText = """start a=node(%A%) return coalesce(a.hairColour?, a.eyes?)""",
      returns = """""",
      assertions = (p) => assert(Seq("brown") === p.columnAs[String]("coalesce(a.hairColour?, a.eyes?)").toSeq)
    )
  }

  @Test def abs() {
    testThis(
      title = "ABS",
      syntax = "ABS( expression )",
      arguments = List("expression" -> "A numeric expression."),
      text = "`ABS` returns the absolute value of a number.",
      queryText = """start a=node(%A%), c=node(%E%) return a.age, c.age, abs(a.age - c.age)""",
      returns = "The absolute value of the age difference is returned.",
      assertions = (p) => assert(List(Map("abs(a.age - c.age)"->3.0, "a.age"->38, "c.age"->41)) === p.toList)
    )
  }

  @Test def round() {
    testThis(
      title = "ROUND",
      syntax = "ROUND( expression )",
      arguments = List("expression" -> "A numerical expression."),
      text = "`ROUND` returns the numerical expression, rounded to the nearest integer.",
      queryText = """start a=node(1) return round(3.141592)""",
      returns = "",
      assertions = (p) => assert(List(Map("round(3.141592)"->3)) === p.toList)
    )
  }

  @Test def sqrt() {
    testThis(
      title = "SQRT",
      syntax = "SQRT( expression )",
      arguments = List("expression" -> "A numerical expression"),
      text = "`SQRT` returns the square root of a number.",
      queryText = """start a=node(1) return sqrt(256)""",
      returns = "",
      assertions = (p) => assert(List(Map("sqrt(256)"->16))=== p.toList)
    )
  }

  @Test def sign() {
    testThis(
      title = "SIGN",
      syntax = "SIGN( expression )",
      arguments = List("expression" -> "A numerical expression"),
      text = "`SIGN` returns the signum of a number -- zero if the expression is zero, `-1` for any negative number, and `1` for any positive number.",
      queryText = "start n=node(1) return sign(-17), sign(0.1)",
      returns = "",
      assertions = (p) => assert(List(Map("sign(-17)"-> -1, "sign(0.1)"->1)) === p.toList)
    )
  }

  @Test def range() {
    testThis(
      title = "RANGE",
      syntax = "RANGE( start, end [, step] )",
      arguments = List(
        "start" -> "A numerical expression.",
        "end" -> "A numerical expression.",
        "step" -> "A numerical expression."
      ),
      text = "Returns numerical values in a range with a non-zero step value step. Range is inclusive in both ends.",
      queryText = "start n=node(1) return range(0,10), range(2,18,3)",
      returns = "Two lists of numbers are returned.",
      assertions = (p) => assert(List(Map(
        "range(0,10)"-> List(0,1,2,3,4,5,6,7,8,9,10),
        "range(2,18,3)"->List(2,5,8,11,14,17)
      )) === p.toList)
    )
  }
  private def testThis(title: String, syntax: String, arguments: List[(String, String)], text: String, queryText: String, returns: String, assertions: (ExecutionResult => Unit)*) {
    val argsText = arguments.map(x => "* _" + x._1 + ":_ " + x._2).mkString("\r\n\r\n")
    val fullText = String.format("""%s

*Syntax:* `%s`

*Arguments:*

%s""", text, syntax, argsText)
    testQuery(title, fullText, queryText, returns, assertions: _*)
  }
}
