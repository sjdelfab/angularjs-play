package scalaext

/**
 * Acknowledgment: http://stackoverflow.com/questions/5654004/implementing-iftrue-iffalse-ifsome-ifnone-etc-in-scala-to-avoid-if-and
 */
class OptionExt[A](option: Option[A]) {
  def ifNone[R](f: => R) = new Otherwise1(option match {
    case None => Some(f)
    case Some(_) => None
  }, option.get)
  def ifSome[R](f: A => R) = new Otherwise0(option match {
    case Some(value) => Some(f(value))
    case None => None
  })
}

object OptionExt {
  import scala.language.implicitConversions
  implicit def extendOption[A](opt: Option[A]): OptionExt[A] = new OptionExt[A](opt)
  
}

class Otherwise0[R](intermediateResult: Option[R]) {
  def otherwise[S >: R](f: => S) = intermediateResult.getOrElse(f)
  def apply[S >: R](f: => S) = otherwise(f)
}

class Otherwise1[R, A1](intermediateResult: Option[R], arg1: => A1) {
  def otherwise[S >: R](f: A1 => S) = intermediateResult.getOrElse(f(arg1))
  def apply[S >: R](f: A1 => S) = otherwise(f)
}