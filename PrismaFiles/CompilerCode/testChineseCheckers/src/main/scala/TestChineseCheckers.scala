import prisma.meta.BangNotation.↓
import prisma.runner.{Account, ganache}
import prisma.Prisma._

import java.math.BigInteger

object TestChineseCheckers {
  def run(ctor: (Account, Address, Arr[AddressPayable]) => CCContracts.ChineseCheckers,
          pl1: Account, pl2: Account, pl3: Account): (CCContracts.ChineseCheckers, CCContracts.ChineseCheckers, CCContracts.ChineseCheckers) = {
    val deploy = ctor(pl1, "0".a, new Arr(pl1.credentials.getAddress.p, pl2.credentials.getAddress.p, pl3.credentials.getAddress.p))

    val x = ctor(pl1, deploy.addr, null) // load from addr
    val y = ctor(pl2, deploy.addr, null) // load from addr
    val z = ctor(pl3, deploy.addr, null) // load from addr
    val a: Thread = new Thread({ () => x.trampoline("()") })
    val b: Thread = new Thread({ () => y.trampoline("()") })
    val c: Thread = new Thread({ () => z.trampoline("()") })
    a.start(); b.start(); c.start()
    a.join(); b.join(); c.join()
    (x, y, z)
  }

  def main(args: Array[String]): Unit =
    run(new CCContracts.ChineseCheckers(_,_,_), ganache.defaultAccount1, ganache.defaultAccount2, ganache.defaultAccount3)
}

@prisma object CCContracts {
  class ChineseCheckers(@cl acc$: Account, @cl addr$: Address, @co players$: Arr[AddressPayable])  { @co @cross var players: Arr[AddressPayable] = players$
    @co @cl class Move(var root: U256, var directions: Arr[U8])
    @co @cl class StepResult(var pos: U256, var leaped: Boolean)
    @co @cl class Coordinate(var dir: U256, var length1: U256, var length2: U256)

    @co @cross val board: U8 x 121 = new (U8 x 121)(
      "0".u8,"0".u8,"0".u8,"0".u8,"1".u8,"1".u8,"0".u8,"0".u8,"1".u8,"1".u8,"1".u8,"0".u8,"1".u8,"1".u8,"1".u8,"1".u8,"1".u8,"1".u8,"1".u8,"1".u8,"1".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"1".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"2".u8,"2".u8,"0".u8,"0".u8,"2".u8,"2".u8,"2".u8,"0".u8,"2".u8,"2".u8,"2".u8,"2".u8,"2".u8,"2".u8,"2".u8,"2".u8,"2".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"2".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"3".u8,"3".u8,"0".u8,"0".u8,"3".u8,"3".u8,"3".u8,"0".u8,"3".u8,"3".u8,"3".u8,"3".u8,"3".u8,"3".u8,"3".u8,"3".u8,"3".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"0".u8,"3".u8,"0".u8,"0".u8,"0".u8,"0".u8
    )
    @co @cross var moves: U256 = "2".u

    @cl def input(s: String): String = scala.io.StdIn.readLine(s)
    @cl def printIt(): Unit = {
      println("moves: %s\n  board: %s".format(
        this.moves, this.board))
    }

    @co val init: Unit = {

      while (!goalCheck((moves % "3".u) + "1".u)) {

        moves = moves + "1".u

        val move: Move = ↓(client("move", players(moves % "3".u), { () =>
          printIt()
          ("0".u, new Move(input("start position?: ").u
            , Arr.fromSeq(input("directions: ").split(",").toSeq.map(x => new U8(BigInteger.valueOf(Integer.parseInt(x)))))))
        }))

        require(board(move.root) == U8.from((moves % "3".u) + "1".u), "Not the players token")

        var _aux: StepResult = new StepResult(move.root, false)
        var _i: U256 = "0".u

        while(_i < move.directions.length){

          _aux = testStep(_aux.pos, move.directions(_i))

          if(_aux.pos == "122".u8){
            revert("Moved out of the board");
          }

          if(_aux.leaped == false && move.directions.length != "1".u){
            revert("No leap but multiple moves");
          }

          _i = _i + "1".u

        }

        //Execute move
        board(move.root) = "0".u8
        board(_aux.pos) = U8.from((moves % "3".u)) + "1".u8
      }
      ()
    }

    @co def goalCheck(_player: U256):Boolean = {
      var checkDirection: U256 = "0".u
      var _win: Boolean = true
      if (_player == "1".u) checkDirection = "3".u
      else if (_player == "2".u) checkDirection = "5".u
      else checkDirection = "1".u

      //Check, if every field in player's target corner is occupied by the player:
      //  First directions runs down from 4 to 1
      //  Second direction runs down from 4 to (4 -first direction)
      var b = "4".u
      while (b >= "1".u) {
        var d: U256 = "5".u - b - "1".u
        while ( d <= "4".u) { //uint cant get below 0, therefore # d >= (4-b); d--# would be endless
          if (board(coordinatesToIndex(checkDirection, b, d)) != _player) {
            _win = false
            d = "4".u
            b = "1".u
          }
          d += "1".u
        }
        b -= "1".u
      }

      //The corner-stone of the next triangle needs to be checked as well:
      if (_win && board(coordinatesToIndex((checkDirection + "1".u) % "6".u, "4".u, "0".u)) != _player) {
        _win = false
      }

      _win
    }

    @co def testStep(_pos : U256, _dir: U8): StepResult = {

      //Return variables
      var _newPos: U256 = getHopTarget(_pos, U256.from(_dir))
      var _leaped: Boolean = false

      if (_newPos != "122".u && board(_newPos) != "0".u8) {

        _newPos = getHopTarget(_newPos, U256.from(_dir))

        if (_newPos != "122".u && board(_newPos) != "0".u8) {
          _newPos = "122".u
        } else {
          _leaped = true
        }
      }
      new StepResult(_newPos, _leaped)
    }

    @co def getHopTarget(_pos: U256, _dir: U256): U256 = {

      //Convert position in array to coordinate
      val _pre: Coordinate = indexToCoordinate(_pos)

      //Check if direction is right
      if(_dir > "5".u){"122".u}
      //Check if position is center
      else if(_pre.dir + _pre.length1 + _pre.length2 == "0".u){ coordinatesToIndex(_dir ,"1".u,"0".u);}
      //Check if direction is 0 degree from base direction
      else if(_dir  == _pre.dir){
        // (x,y+1,z)
        coordinatesToIndexChecked(_pre.dir, (_pre.length1 + "1".u), _pre.length2);
      }
      //Check if direction is 60 degree from base direction
      else if(_dir  == ((_pre.dir+"1".u) % "6".u)){
        // (x,y,z+1)
        coordinatesToIndexChecked(_pre.dir , _pre.length1 , _pre.length2 + "1".u);
      }
      //Check if direction is 120 degree from base direction
      else if(_dir  == ((_pre.dir+"2".u)%"6".u)){
        if(_pre.length1 == "1".u){
          //New base direction: (x+1%"6".u,z,0)
          coordinatesToIndexChecked(((_pre.dir+"1".u)%"6".u) , (_pre.length2 + "1".u) , "0".u);
        } else {
          //Same base direction (x,y-1,z+1)
          coordinatesToIndexChecked(_pre.dir , (_pre.length1 - "1".u) , _pre.length2 + "1".u);
        }
      }
      //Check if direction is 180 degree from base direction
      else if(_dir  == ((_pre.dir+"3".u)%"6".u)){
        if(_pre.length1 == "1".u){
          if(_pre.length2 == "0".u){
            //Go to center: 0
            "0".u;
          } else {
            //New base direction: (x+1%"6".u,z,0)
            coordinatesToIndex(((_pre.dir+"1".u)%"6".u), _pre.length2, "0".u);
          }
        } else {
          //Same base direction (x,y-1,z)
          coordinatesToIndex(_pre.dir, (_pre.length1 - "1".u), _pre.length2);
        }
      }
      //Check if direction is 240 degree from base direction
      else if(_dir  == ((_pre.dir+"4".u)%"6".u)){
        if(_pre.length2 == "0".u){
          //New base direction: (x-1%"6".u, 1, y-1)
          coordinatesToIndex(((_pre.dir+"5".u)%"6".u),"1".u,_pre.length1 - "1".u);
        } else {
          //Same base direction: (x,y,z-1)
          coordinatesToIndex(_pre.dir, _pre.length1, _pre.length2 - "1".u);
        }
      }
      //Check if direction is 300 degree from base direction
      else if(_dir  == ((_pre.dir+"5".u)%"6".u)){
        if(_pre.length2 == "0".u){
          //New base direction: (x-1%"6".u, 1, y)
          coordinatesToIndex(((_pre.dir+"5".u)%"6".u),"1".u, _pre.length1);
        } else {
          //Same base direction: (x,y+1,z-1)
          coordinatesToIndex(_pre.dir,(_pre.length1 + "1".u), _pre.length2 - "1".u);
        }
      }
      else {
        "122".u
      }
    }

    @co def indexToCoordinate(_index: U256): Coordinate = {
      require(_index < "121".u);

      if(_index == "0".u){
        new Coordinate("0".u,"0".u,"0".u);
      } else {
        val _length2: U256 = (_index - "1".u) % "5".u;
        val _length1: U256 = (((_index - "1".u - _length2) / "5".u ) % "4".u) + "1".u;
        val _dir: U256 = ((_index - "1".u) - ((_index - "1".u) % "20".u)) / "20".u;
        new Coordinate(_dir, _length1, _length2)
      }


    }

    @co def coordinatesToIndex(_dir:U256, _l1: U256, _l2: U256): U256 = {
      _dir * "20".u + (_l1 - "1".u) * "5".u + (_l2 + "1".u)
    }

    @co def coordinatesToIndexChecked(_dir:U256, _l1: U256, _l2: U256): U256 = {
      //Check if position exists:
      if(_dir > "5".u || _l1 == "0".u || _l1 > "4".u || _l2 > "4".u){
        revert("Jump to invalid coordinate");
      }
      coordinatesToIndex(_dir, _l1, _l2);
    }
  }
}