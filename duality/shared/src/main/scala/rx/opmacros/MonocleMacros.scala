package monocle.macros

import monocle.Lens
import monocle.macros.internal.MacroImpl

import scala.reflect.macros.blackbox

// class MonocleDuality(val c: blackbox.Context) {
object MonocleDuality {
  def genLens(s:String): Int
  // def genLens[S: c.WeakTypeTag, A: c.WeakTypeTag](c: blackbox.Context)(field: c.Expr[S => A]): c.Expr[Lens[S, A]] = {
  //   val impl = new MacroImpl(c)
  //   impl.genLens_impl[S, A](field.asInstanceOf[impl.c.Expr[S => A]]).asInstanceOf[c.Expr[Lens[S,A]]] //TODO: better?
  // }
}
