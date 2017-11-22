package rx

import scala.util.Try

trait Var[T] extends Rx[T] {
  def update(newValue: T): Unit

  private[rx] var value: T
}

object Var {
  /**
    * Create a [[Var]] from an initial value
    */
  def apply[T](initialValue: T): Var[T] = new BaseVar(initialValue)

  /**
    * Set the value of multiple [[Var]]s at the same time; in doing so,
    * reduces the redundant updates that would normally occur setting
    * them one by one
    */
  def set(args: VarTuple[_]*) = {
    args.foreach(_.set())
    Rx.doRecalc(
      args.flatMap(_.v.downStream),
      args.flatMap(_.v.observers)
    )
  }

}

/**
  * A smart variable that can be set manually, and will notify downstream
  * [[Rx]]s and run any triggers whenever its value changes.
  */
class BaseVar[T](initialValue: T) extends Var[T] {

  private[rx] def depth = 0

  private[rx] var value = initialValue

  override def now = value

  def toTry = util.Success(now)

  /**
    * Sets the value of this [[Var]] and runs any triggers/notifies
    * any downstream [[Rx]]s to update
    */
  def update(newValue: T): Unit = {
    if (value != newValue) {
      value = newValue
      Rx.doRecalc(downStream.toSet, observers)
    }
  }

  private[rx] override def recalc(): Unit = propagate()

  private[rx] override def kill() = {
    clearDownstream()
  }

  override def toString() = s"Var@${Integer.toHexString(hashCode()).take(2)}($now)"
}

class IsomorphicVar[T, S](base: Var[T], read: T => S, write: S => T)(implicit ownerCtx: Ctx.Owner) extends Var[S] {

  //  private[rx] val rx = base.map(read)
  //  private[rx] val rx = Rx{ read(base()) }
  private[rx] val rx = Rx.build { (ownerCtx, dataCtx) =>
    base.addDownstream(dataCtx)
    read(base()(dataCtx))
  }(ownerCtx)

  // Rx
  override def now: S = rx.now

  override def toTry: Try[S] = rx.toTry

  override private[rx] def kill(): Unit = rx.kill()

  override private[rx] def recalc(): Unit = rx.recalc()

  override private[rx] def depth = rx.depth


  // Var
  override def update(newValue: S): Unit = base.update(write(newValue))

  override private[rx] def value = rx.now

  override private[rx] def value_=(newValue: S) = base.value = write(newValue)
}

case class VarTuple[T](v: Var[T], value: T) {
  def set() = v.value = value
}

/**
  * Encapsulates the act of setting of a [[Var]] to a value, without
  * actually setting it.
  */
object VarTuple {
  implicit def tuple2VarTuple[T](t: (Var[T], T)): VarTuple[T] = {
    VarTuple(t._1, t._2)
  }

  implicit def tuples2VarTuple[T](ts: Seq[(Var[T], T)]): Seq[VarTuple[T]] = {
    ts.map(t => VarTuple(t._1, t._2))
  }
}
