import prisma.meta.BangNotation._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
//import scala.language.higherKinds
import scala.util.Try

object repeat extends DSL with IF {
  type M[A] = Iterable[A]
  type N[A] = Iterable[A]

  override def make[X](x: X): Iterable[X] = Option(x)
  override def flatMap[X, Y](x: Iterable[X], f: X => Iterable[Y]): Iterable[Y] =
    x.flatMap(f)
  override def IF_[A](x: Boolean)(y: () => Iterable[A])(z: () => Iterable[A]): Iterable[A] =
    if (x) y() else z()
}

object async extends DSL with IF {
  type M[A] = Future[A]
  type N[A] = Future[A]

  override def make[X](x: X): Future[X] = Future.successful(x)
  override def flatMap[X, Y](x: Future[X], f: X => Future[Y]): Future[Y] =
    x.flatMap(f)(scala.concurrent.ExecutionContext.global)
  override def IF_[A](x: Boolean)(y: () => Future[A])(z: () => Future[A]): Future[A] =
    if (x) y() else z()
}

class Contract[X](val value: X) {}
object Contract extends DSL with IF with WHILE {
  type M[A] = Contract[A]
  type N[A] = Contract[A]

  override def make[X](x: X): Contract[X] = new Contract(x)
  override def flatMap[X, Y](x: Contract[X], f: X => Contract[Y]): Contract[Y] =
    f(x.value)
  override def IF_[A](x: Boolean)(y: () => Contract[A])(z: () => Contract[A]): Contract[A] =
    if (x) y() else z()
  override def WHILE_(x: () => Contract[Boolean])(y: () => Contract[Unit]): Contract[Unit] =
    new Contract(while (x().value) y())
  object client {
    def apply[A](addr: Int)(f: => A): Contract[A] = new Contract(f) }
}

class TestBang extends munit.FunSuite {

  test("bang") { val (a, b) = Toast.bang(); assertEquals(a, b) }
  test("testy") { assertEquals(Toast.testy(), 5) }
  test("for3") { assertEquals(Toast.for3(), Iterable(1)) }
  test("contract") { assertEquals(Toast.contract(), "x=12 y=12 z=7") }

}

@BangNotation2 object Toast {

  def bang(): (Iterable[Int], Iterable[Int]) = {
    import repeat._

    val baz = (x: Int) => List(x, x+1, x+2)
    val bar = List(1,4,7)
    val boz = List(10,13,16)
    val biz = List(0,1,2)

    val lst = (x: Int) => Iterable(x)
    def pair(x: Int, y: Int) = List(x,y)
    val result: Iterable[Int] = repeat {
      if (↓(pair(0,1)) == 0)  { ↓(baz(↓(bar))) }
      else  { ↓(boz) + ↓(biz) }
    }

    val result2: List[Int] = printIt {
      val foo = List(0,1)
      for {
        x <- foo
        result <- IF (x == 0) { () =>
          for {
            a1 <- bar
            a2 <- baz(a1)
          } yield a2
        } { () =>
          for {
            b1 <- boz
            b2 <- biz
          } yield b1 + b2
        }
      } yield result
    }

    (result, result2)
  }


  def testy(): Int = {
    import async._

    val result3: Future[Int] = async {
      if (↓(async(0)) == 0) {
        ↓(async(5))
      } else
        3
    }
    Await.result(result3, Duration(1, "seconds"))
  }


  def for3(): Iterable[Int] = {
    import repeat._

    val result2: Iterable[Int] = repeat {
      val x = Try(10 / 2).toOption.toIterable
      if (↓(x) > 5) {
        ↓(x)
      } else {
        1
      }
    }

    result2
  }


  def contract(): String = {
    import Contract._

    var x = 0
    var y = Contract { 0 }
    def incr: Contract[Unit] = Contract { x = x+1 }
    def result: Contract[String] = Contract {
      ↓(incr)
      y = Contract { ↓(y) + 1 }
      var c = 0
      while ({c = c+1; c <= 10}) {
        ↓(incr)
        y = Contract { ↓(y) + 1 }
      }
      ↓(incr)
      y = Contract { ↓(y) + 1 }
      val z = ↓(client(12){ /* println("x"); */ 7 })
      "x=" + x + " y=" + ↓(y) + " z=" + z
    }

    result.value
  }

}
