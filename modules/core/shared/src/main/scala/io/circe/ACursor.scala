package io.circe

import cats.Applicative
import cats.kernel.Eq
import java.io.Serializable

/**
 * A zipper that represents a position in a JSON document and supports navigation and modification.
 *
 * The `focus` represents the current position of the cursor; it may be updated with `withFocus` or
 * changed using navigation methods like `left` and `right`.
 *
 * @groupname Utilities Miscellaneous utilities
 * @groupprio Utilities 0
 * @groupname Access Access and navigation
 * @groupprio Access 1
 * @groupname Modification Modification
 * @groupprio Modification 2
 * @groupname ArrayAccess Array access
 * @groupprio ArrayAccess 3
 * @groupname ObjectAccess Object access
 * @groupprio ObjectAccess 4
 * @groupname ArrayNavigation Array navigation
 * @groupprio ArrayNavigation 5
 * @groupname ObjectNavigation Object navigation
 * @groupprio ObjectNavigation 6
 * @groupname ArrayModification Array modification
 * @groupprio ArrayModification 7
 * @groupname ObjectModification Object modification
 * @groupprio ObjectModification 8
 * @groupname Decoding Decoding
 * @groupprio Decoding 9
 *
 * @author Travis Brown
 */
abstract class ACursor(private val lastCursor: HCursor, private val lastOp: CursorOp)
    extends ReadCursor(lastCursor, lastOp)
    with Serializable {

  /**
   * The current location in the document.
   *
   * @group Access
   */
  def focus: Option[Json]

  /**
   * Indicate whether this cursor represents the result of a successful
   * operation.
   *
   * @group Decoding
   */
  def succeeded: Boolean

  /**
   * Return the cursor as an [[HCursor]] if it was successful.
   *
   * @group Decoding
   */
  def success: Option[HCursor]

  /**
   * Return to the root of the document.
   *
   * @group Access
   */
  def top: Option[Json]

  /**
   * Return the cursor to the root of the document.
   *
   * @group Access
   */
  override def root: HCursor = null

  /**
   * Modify the focus using the given function.
   *
   * @group Modification
   */
  def withFocus(f: Json => Json): ACursor

  /**
   * Modify the focus in a context using the given function.
   *
   * @group Modification
   */
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor]

  /**
   * Replace the focus.
   *
   * @group Modification
   */
  final def set(j: Json): ACursor = withFocus(_ => j)

  /**
   * If the focus is a JSON array, return its elements.
   *
   * @group ArrayAccess
   */
  def values: Option[Iterable[Json]]

  /**
   * If the focus is a value in a JSON object, return the key.
   *
   * @group ArrayAccess
   */
  override def index: Option[Int] = None

  /**
   * If the focus is a JSON object, return its field names in their original order.
   *
   * @group ObjectAccess
   */
  override def keys: Option[Iterable[String]]

  /**
   * If the focus is a value in a JSON object, return the key.
   *
   * @group ObjectAccess
   */
  override def key: Option[String] = None

  /**
   * Delete the focus and move to its parent.
   *
   * @group Modification
   */
  def delete: ACursor

  /**
   * Move the focus to the parent.
   *
   * @group Access
   */
  def up: ACursor

  /**
   * If the focus is an element in a JSON array, move to the left.
   *
   * @group ArrayNavigation
   */
  def left: ACursor

  /**
   * If the focus is an element in a JSON array, move to the right.
   *
   * @group ArrayNavigation
   */
  def right: ACursor

  /**
   * If the focus is a JSON array, move to its first element.
   *
   * @group ArrayNavigation
   */
  def downArray: ACursor

  /**
   * If the focus is a JSON array, move to the element at the given index.
   *
   * @group ArrayNavigation
   */
  def downN(n: Int): ACursor

  /**
   * If the focus is a value in a JSON object, move to a sibling with the given key.
   *
   * @group ObjectNavigation
   */
  def field(k: String): ACursor

  /**
   * If the focus is a JSON object, move to the value of the given key.
   *
   * @group ObjectNavigation
   */
  def downField(k: String): ACursor

  /**
   * Replay an operation against this cursor.
   *
   * @group Utilities
   */
  override final def replayOne(op: CursorOp): ACursor = op match {
    case CursorOp.MoveLeft       => left
    case CursorOp.MoveRight      => right
    case CursorOp.MoveUp         => up
    case CursorOp.Field(k)       => field(k)
    case CursorOp.DownField(k)   => downField(k)
    case CursorOp.DownArray      => downArray
    case CursorOp.DownN(n)       => downN(n)
    case CursorOp.DeleteGoParent => delete
  }

  /**
   * Replay history (a list of operations in reverse "chronological" order) against this cursor.
   *
   * @group Utilities
   */
  final override def replay(history: List[CursorOp]): ACursor = history.foldRight(this)((op, c) => c.replayOne(op))
}

object ACursor {
  private[this] val jsonOptionEq: Eq[Option[Json]] = cats.kernel.instances.option.catsKernelStdEqForOption(Json.eqJson)

  implicit val eqACursor: Eq[ACursor] =
    Eq.instance((a, b) => jsonOptionEq.eqv(a.focus, b.focus) && CursorOp.eqCursorOpList.eqv(a.history, b.history))
}
