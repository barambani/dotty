import scala.quoted._
import dotty.tools.dotc.quoted.Toolbox._

import scala.tasty.Universe
import scala.tasty.Tasty
import scala.tasty.util.{TreeTraverser, Show}

object Macros {

  implicit inline def printOwners[T](x: => T): Unit =
    ~impl('(x))(Universe.compilationUniverse) // FIXME infer Universe.compilationUniverse within top level ~

  def impl[T](x: Expr[T])(implicit u: Universe): Expr[Unit] = {
    import u._
    import u.tasty._


    val buff = new StringBuilder

    val output = new TreeTraverser(u.tasty) {
      override def traverseTree(tree: Tree)(implicit ctx: Context): Unit = {
        // Use custom Show[_] here
        implicit val printer = new DummyShow(tasty)
        tree match {
          case tree @ DefDef(name, _, _, _, _) =>
            buff.append(name)
            buff.append("\n")
            buff.append(tree.owner.show)
            buff.append("\n\n")
          case tree @ ValDef(name, _, _) =>
            buff.append(name)
            buff.append("\n")
            buff.append(tree.owner.show)
            buff.append("\n\n")
          case _ =>
        }
        traverseTreeChildren(tree)
      }
    }

    val tree = x.toTasty
    output.traverseTree(tree)
    '(print(~buff.result().toExpr))
  }

}

class DummyShow[T <: Tasty with Singleton](tasty0: T) extends Show[T](tasty0) {
  import tasty._
  def showTree(tree: Tree)(implicit ctx: Context): String = "Tree"
  def showTypeOrBoundsTree(tpt: TypeOrBoundsTree)(implicit ctx: Context): String = "TypeOrBoundsTree"
  def showTypeOrBounds(tpe: TypeOrBounds)(implicit ctx: Context): String = "TypeOrBounds"
  def showConstant(const: Constant)(implicit ctx: Context): String = "Constant"
}
