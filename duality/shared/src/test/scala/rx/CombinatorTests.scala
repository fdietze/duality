package rx

//
import utest._

import scala.util.{Failure, Success, Try}

object CombinatorTests extends TestSuite{

  object TopLevelVarCombinators {
    val aa = Var(1)

    val mapped = aa.map(_ + 10)
  }

  def tests = utest.Tests {
    "combinators" - {
      import Ctx.Owner.Unsafe._
      "foreach" - {
        val a = Var(1)
        var count = 0
        val o = a.foreach{ x =>
          count = x + 1
        }
        assert(count == 2)
        a() = 4
        assert(count == 5)
      }
      "map" - {
        val a = Var(10)
        val b = Rx{ a() + 2 }
        val c = a.map(_*2)
        val d = b.map(_+3)
        val e = a.map(_*2).map(_+3)
        assert(c.now == 20)
        assert(d.now == 15)
        assert(e.now == 23)
        a() = 1
        assert(c.now == 2)
        assert(d.now == 6)
        assert(e.now == 5)
      }
      "mapAll" - {
        val a = Var(10L)
        val b = Rx{ 100 / a() }
        val c = b.all.map{
          case Success(x) => Success(x * 2)
          case Failure(_) => Success(1337)
        }
        val d = b.all.map{
          case Success(x) => Failure(new Exception("No Error?"))
          case Failure(x) => Success(x.toString)
        }
        assert(c.now == 20)
        assert(d.toTry.isFailure)
        a() = 0
        assert(c.now == 1337)
        assert(d.toTry == Success("java.lang.ArithmeticException: / by zero"))
      }
      "killRx" - {
        val (a, b, c, d, e, f) = Utils.initGraph

        assert(c.now == 3)
        assert(e.now == 7)
        assert(f.now == 26)
        a() = 3
        assert(c.now == 5)
        assert(e.now == 9)
        assert(f.now == 38)

        // Killing d stops it from updating, but the changes can still
        // propagate through e to reach f
        d.kill()
        a() = 1
        assert(f.now == 36)

        // After killing f, it stops updating but others continue to do so
        f.kill()
        a() = 3
        assert(c.now == 5)
        assert(e.now == 9)
        assert(f.now == 36)

        // After killing c, the everyone doesn't get updates anymore
        c.kill()
        a() = 1
        assert(c.now == 5)
        assert(e.now == 9)
        assert(f.now == 36)
      }
    }
    'higherOrder - {
      import Ctx.Owner.Unsafe._
      "map" - {
        val v = Var(Var(1))
        val a = v.map(_() + 42)
        assert(a.now == 43)
        v.now() = 100
        assert(a.now == 142)
        v() = Var(3)
        assert(a.now == 45)

        //Ensure this thing behaves in some normal fashion
        val vv = Var(Rx(Var(1)))
        val va = vv.map(aa => "a" * aa.now.now)
        assert(va.now == "a")
        vv.now.now() = 2
        assert(va.now == "a")
        vv() = Rx(Var(3))
        assert(va.now == "aaa")
        vv() = Rx(Var(4))
        assert(va.now == "aaaa")
      }
    }
    "topLevelCombinators" - {
      import TopLevelVarCombinators._
      assert(mapped.now == 11)
      aa() = 2
      assert(mapped.now == 12)
      aa() = 3
      assert(mapped.now == 13)
    }
  }
}
