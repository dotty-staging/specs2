package org.specs2
package data

import org.specs2.fp._
import Tree._

/**
 * Utility methods for Trees
 */
trait Trees { outer =>

  /**
   * extension methods for the Tree trait
   */
  extension [A, B](t: Tree[A])
    def bottomUp(f: ((A, LazyList[B]) => B)) = outer.bottomUp(t, f)
    def prune(f: A => Option[B]): Option[Tree[B]] = outer.prune(t, f)

  extension [A](t: Tree[A])
    def prune(f: Tree[A] => Option[A])(using initial: A): Tree[A] = outer.prune(t, f)
    def flattenSubForests = outer.flattenSubForests(t)
    def flattenLeft       = outer.flattenLeft(t)
    def size              = t.flatten.size
    def allPaths          = outer.allPaths(t)

  /**
   * This implicit can be used to remove None nodes in a Tree
   */
  extension [A](t: Tree[Option[A]])
    def clean(using initial: A): Tree[A] = outer.clean(t)

  /**
   * map a Tree from leaves to root by replacing each node with the result of a function taking
   * that node and the mapping of all its children.
   *
   * This is used in JUnit to map a Tree[Description] where no Description objects are related to a Tree[Description]
   * where each node returns the children nodes on the "getChildren" method
   */
  def bottomUp[A, B](t: Tree[A], f: ((A, LazyList[B]) => B)): Tree[B] =
    val tbs = t.subForest.map(t => bottomUp(t, f))
    Node(f(t.rootLabel, tbs.map(_.rootLabel)), tbs)

  /**
   * remove None nodes from a tree
   */
  def clean[A](t: Tree[Option[A]])(using initial: A): Tree[A] =
    prune(t, (a: Option[A]) => a).getOrElse(Leaf(initial))

  /**
   * remove nodes from a tree if they are None according to a function f
   */
  def prune[A, B](t: Tree[A], f: A => Option[B]): Option[Tree[B]] =
    val tbs = t.subForest.flatMap(t => prune(t, f))
    f(t.rootLabel).map { root =>
      Node(root, tbs)
    }

  /**
   * remove nodes from a tree if they are None according to a function f
   */
  def prune[A](t: Tree[A], f: Tree[A] => Option[A])(using initial: A): Tree[A] = t.cobind(f).clean

  def flattenSubForests[A](tree: Tree[A]): Tree[A] = Node(tree.rootLabel, tree.flattenLeft.drop(1).map(Leaf(_)))

  /**
   * flatten the tree using a foldLeft to avoid SOF
   */
  def flattenLeft[A](tree: Tree[A]): LazyList[A] = squishLeft(tree, LazyList.empty)

  /** reimplementation of squish from scalaz, using a foldLeft */
  private def squishLeft[A](tree: Tree[A], xs: LazyList[A]): LazyList[A] =
    LazyList.cons(tree.rootLabel, tree.subForest.reverse.foldLeft(xs)((s, t) => squishLeft(t, s)))

  /**
   * Implicit definition to add more functionalities to the TreeLoc class
   */
  extension [T](t: TreeLoc[T])
    def parentLocs = outer.parentLocs(t)
    def size = outer.size(t)
    def getParent = t.parent.getOrElse(t)
    def updateLabel(f: T => T) = t.setLabel(f(t.getLabel))
    def addChild(c: T) = t.insertDownLast(Leaf(c)).getParent
    def addFirstChild(c: T) = t.insertDownFirst(Leaf(c)).getParent
    def insertDownLast(c: T) = t.insertDownLast(Leaf(c))

  /**
   * @return the number of nodes in a TreeLoc
   */
  def size[A](t: TreeLoc[A]): Int = t.root.toTree.size

  /**
   * @return all the paths from root to leaves
   */
  def allPaths[A](tree: Tree[A]): List[List[A]] =
    tree.subForest.toList match
      case Nil => List(List(tree.rootLabel))
      case subForests => subForests.flatMap(subTree => allPaths(subTree).map(p => tree.rootLabel :: p))

  /**
   * @return the list of all parent locs from a given TreeLoc
   */
  def parentLocs[T](t: TreeLoc[T], ps: Seq[TreeLoc[T]] = Vector()): Seq[TreeLoc[T]] = t.parent match
    case Some(p) => parentLocs(p, p +: ps)
    case None    => ps

  given treeLocIsSized[T] as Sized[TreeLoc[T]] = new Sized[TreeLoc[T]] {
    def size(t: TreeLoc[T]) : Int = t.size
  }

  given treeIsSized[T] as Sized[Tree[T]] = new Sized[Tree[T]] {
    def size(t: Tree[T]) : Int = t.size
  }
}

object Trees extends Trees
