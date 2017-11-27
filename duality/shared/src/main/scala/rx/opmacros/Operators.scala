package rx.opmacros

import rx.opmacros.Utils._
import rx.Rx

import scala.language.experimental.macros
import scala.reflect.macros._

/**
  * Implementations for the various macros that Scala.rx defines for its operators.
  */
object Operators {
  def initialize(c: whitebox.Context)(f: c.Tree, owner: c.Tree) = {
    import c.universe._
    val data = c.inferImplicitValue(c.weakTypeOf[rx.Ctx.Data])
    val newDataCtx =  c.freshName(TermName("rxDataCtx"))
    val newOwnerCtx =  c.freshName(TermName("rxOwnerCtx"))
    val newFunc2 = doubleInject(c)(f, newOwnerCtx, owner, newDataCtx, data)
    val enclosingCtx = Utils.enclosingCtx(c)(owner)
    val newTree = q"($newOwnerCtx: _root_.rx.Ctx.Owner, $newDataCtx: _root_.rx.Ctx.Data) => $newFunc2"
    (newTree, newOwnerCtx, enclosingCtx)
  }

  def map[T: c.WeakTypeTag, V: c.WeakTypeTag, Wrap[_]]
         (c: whitebox.Context)
         (f: c.Expr[Wrap[T] => Wrap[V]])
         (ownerCtx: c.Expr[rx.Ctx.Owner])
         (implicit w: c.WeakTypeTag[Wrap[_]]): c.Expr[Rx.Dynamic[V]] = {

    import c.universe._
    val (call, newCtx, enclosingCtx) = initialize(c)(f.tree, ownerCtx.tree)

    Utils.resetExpr[Rx.Dynamic[V]](c)(q"""
      ${c.prefix}.macroImpls.mappedImpl($call, $enclosingCtx)
    """)
  }
}

/**
  * Non-macro runtime implementations for the functions that Scala.rx's macros
  * forward to. This is parametrized on a [[Wrap]]per type, to be re-usable for
  * both operators dealing with `T` and `Try[T]`.
  *
  * Provides a small number of helpers to deal with generically dealing with
  * `Wrap[T]`s
  */
trait Operators[T, Wrap[_]]{

  def get[V](t: Rx[V]): Wrap[V]
  def unwrap[V](t: Wrap[V]): V
  def prefix: Rx[T]

  def mappedImpl[V](call: (rx.Ctx.Owner, rx.Ctx.Data) => Wrap[T] => Wrap[V],
                    enclosing: rx.Ctx.Owner): Rx.Dynamic[V] = {

    Rx.build { (ownerCtx, dataCtx) =>
      prefix.addDownstream(dataCtx)
      this.unwrap(call(ownerCtx, dataCtx)(this.get(prefix)))
    }(enclosing)
  }
}
