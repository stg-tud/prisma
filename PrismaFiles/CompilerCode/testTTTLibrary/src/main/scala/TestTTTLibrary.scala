import prisma.runner.Account
import prisma.Prisma._

// !!! LoC Counted !!!

@prisma object Libraries {
  class TTTLibrary(@cl acc$: Account, @cl addr$: Address)  {
    @co @cross @view def checkWin(board: Arr[Arr[U8]], x: U8, y: U8): Unit = {
      if(board(U256.from(x))("0".u) === board(U256.from(x))("1".u) && board(U256.from(x))("1".u) === board(U256.from(x))("2".u)
      || board("0".u)(U256.from(y)) === board("1".u)(U256.from(y)) && board("1".u)(U256.from(y)) === board("2".u)(U256.from(y))
      || board("1".u)("1".u) != "0".u8 && ((board("0".u)("2".u) === board("1".u)("1".u) && board("2".u)("0".u) === board("1".u)("1".u))
      || (board("0".u)("0".u) === board("1".u)("1".u) && board("2".u)("2".u) === board("1".u)("1".u)))) {
        require(true)
      } else {
        require(false)
      }
    }
  }
}