import rx.opmacros.Operators
import rx.opmacros.Utils.Id
import scala.util.Try

/**
 * Created by haoyi on 12/13/14.
 */
package object rx {


  object GenericRxOps{

    class Macros[T](node: Rx[T]) extends Operators[T, Id] {
      def prefix = node
      def get[V](t: Rx[V]) = t.now
      def unwrap[V](t: V) = t
    }
  }
  /**
   * All [[Rx]]s have a set of operations you can perform on them, e.g. `map` or `filter`
   */
  implicit class GenericRxOps[T](val node: Rx[T]) extends AnyVal {
    import scala.language.experimental.macros

    def macroImpls = new GenericRxOps.Macros(node)

    def map[V](f: Id[T] => Id[V])(implicit ownerCtx: Ctx.Owner): Rx.Dynamic[V] = macro Operators.map[T, V, Id]

    def foreach(f: T => Unit)(implicit ownerCtx: Ctx.Owner): Obs = node.trigger(f(node.now))
  }

  implicit class GenericVarOps[T](val node: Var[T]) extends AnyVal {
    def imap[V](read: Id[T] => Id[V])(write: Id[V] => Id[T])(implicit ownerCtx: Ctx.Owner): Var[V] = new IsomorphicVar[T,V](node,read,write)
  }

  object SafeRxOps{
    class Macros[T](node: Rx[T]) extends Operators[T, util.Try] {
      def prefix = node
      def get[V](t: Rx[V]) = t.toTry
      def unwrap[V](t: Try[V]) = t.get
    }
  }

  abstract class SafeRxOps[T](val node: Rx[T]) {
    import scala.language.experimental.macros
    def macroImpls = new SafeRxOps.Macros(node)

    def map[V](f: Try[T] => Try[V])(implicit ownerCtx: Ctx.Owner): Rx.Dynamic[V] = macro Operators.map[T, V, Try]

    def foreach(f: T => Unit)(implicit ownerCtx: Ctx.Owner): Obs = node.trigger(node.toTry.foreach(f))
  }

  /**
   * All [[Rx]]s have a set of operations you can perform on them via `myRx.all.*`,
   * which lifts the operation to working on a `Try[T]` rather than plain `T`s
   */
  implicit class RxPlusOps[T](val r: Rx[T]) {
    object all extends SafeRxOps[T](r)
  }

}