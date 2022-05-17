import prisma.meta.BangNotation.↓
import org.web3j.crypto.Hash.sha3
import prisma.runner.{Account, ganache}
import prisma.Prisma._

import java.math.BigInteger

// !!! LoC Counted !!!

object TestHangman {

  def run(ctor: (Account, Address, Arr[AddressPayable]) => HangContracts.Hangman,
          pl1: Account, pl2: Account): (HangContracts.Hangman, HangContracts.Hangman) = {

    // deploy new Hangman contract ("0".p means deploy new account)
    val deployed = ctor(pl1, "0".p, Arr(pl1.credentials.getAddress.p, pl2.credentials.getAddress.p))

    // to load a previously deployed contract, use an address != 0
    // when address != constructor arguments are ignored for now, thus we set them to null
    val x: HangContracts.Hangman = ctor(pl1, deployed.addr, null)
    val y: HangContracts.Hangman = ctor(pl2, deployed.addr, null)

    val a: Thread = new Thread({ () => x.trampoline("()") })

    val b: Thread = new Thread({ () => y.trampoline("()") })

    a.start(); b.start()
    a.join();  b.join()

    (x, y)
  }

  def main(args: Array[String]): Unit =
    run(new HangContracts.Hangman(_,_,_), ganache.defaultAccount1, ganache.defaultAccount2)

}

@prisma object HangContracts {

  class Hangman(@cl acc$: Account, @cl addr$: Address, @co players$: Arr[AddressPayable]) { @co @cross val players: Arr[AddressPayable] = players$
    @co @cl class UU(val x: U256, val y: U256)
    @co @cl class UArrU(val arr: Arr[U8], val x: U256)

    @co @cross var hashedWord: U256 = "0".u
    //@co @cross val guessed: Arr[U8] = Arr( // FIXME this lines uses dynmically sized arrays
    @co @cross val guessed: U8 x 26 = new (U8 x 26)( // FIXME this line uses statically sized arrays
      "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8,
      "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8,
      "0".u8, "0".u8, "0".u8, "0".u8, "0".u8, "0".u8)
    @co @cross val word: Arr[U8] = Arr[U8]()
    @co @cross var currentGuess: U8 = "0".u8
    @co @cross var missingletters: U256 = "0".u
    @co @cross var tries: U256 = "5".u
    @cl @cross var secretWord: String = "" // every client has a different value
    @cl @cross var nonce: U256 = "0".u

    @cl def input(s: String): String = {
      printIt()
      scala.io.StdIn.readLine(s)
    }

    @co def commit2(hash: U256, wordLength: U256): Unit = {
      hashedWord = hash
      missingletters = wordLength

      var i = "0".u
      while (i < wordLength) {
        word.push("0".u8)
        i += "1".u
      }
    }

    @co def guess2(letter: U8): Unit = {
      require(letter - "65".u8 < "26".u8, "Not a valid letter")
      require(guessed(U256.from(letter - "65".u8)) === "0".u8, "Letter has already been guessed")
      currentGuess = letter
    }

    @co def respond2(hits: Arr[U256]): Unit = {
      var i = "0".u
      while (i < hits.length) {
        require(hits(i) < word.length)
        require(word(hits(i)) === "0".u8, "this letter was hit before")
        word(hits(i)) = currentGuess
        i = i + "1".u
      }
      guessed(U256.from(currentGuess - "65".u8)) = "1".u8 //Set letter as guessed
      if (hits.length === "0".u) {
        tries = tries - "1".u
      } else {
        missingletters = missingletters - hits.length
      }
    }

    @co def open2(opening: UArrU): Unit = {
      require(opening.arr.length === word.length, "needs word len letters")
      var i = "0".u
      while (i < word.length) {
        if (opening.arr(i) - "65".u8 >= "26".u8
        || guessed(U256.from(opening.arr(i) - "65".u8)) === "1".u8 && !(word(i) === opening.arr(i))) {
          missingletters = "0".u
        }
        i = i + "1".u
      }
      if (keccak256(opening) != hashedWord) {
        missingletters = "0".u
      }
    }

    @cl def printIt(): Unit = {
      val word = this.word
      val guessed = this.guessed
      println("state: attempts=%s, guessed=%s, word=%s, missing=%s,\n letter=%s, hashedWord=%s,\n parties=%s".format( this.tries,
        (0 until guessed.length.bint.intValue()) map (i => if ("1".u8 === guessed(i.toString.u)) (i + 65).asInstanceOf[Char] else '_') mkString "",
        (0 until word.length.bint.intValue()) map (i => word(i.toString.u).bint.intValue().asInstanceOf[Char]) map (c => if (c == 0) '_' else c) mkString "",
        this.missingletters,  this.currentGuess.bint.intValue().asInstanceOf[Char],  this.hashedWord, this.players)
      )
    }

    @co val init: Unit = {

      val pair: UU = ↓(client("commit", players("0".u), { () =>
        secretWord = input("player1 secret: ")
        nonce = new U256(BigInteger.valueOf((Math.random() * 256).toInt))
        // +1 --> parse bytes to positive BigInteger
        val seq = secretWord.map(x => new U8(BigInteger.valueOf(x.asInstanceOf[Int])))
        val hash1 = new U256(new BigInteger(+1, sha3(abiEncode(new UArrU(Arr.fromSeq(seq),nonce)))))
        ("0".u, new UU(hash1, new U256(BigInteger.valueOf(secretWord.length))))
      }))
      commit2(pair.x, pair.y)

      while (tries > "0".u && missingletters != "0".u) {
        guess2(↓(client("guess", players("1".u), { () =>
          ("0".u, new U8(BigInteger.valueOf(input("player2 guess: ")(0).asInstanceOf[Int])))
        })))

        respond2(↓(client("respond", players("0".u), { () =>
          ("0".u, Arr.fromSeq(input("player1 hits: ").split(",").toSeq.map(x => new U256(BigInteger.valueOf(Integer.parseInt(x))))))
        })))
      }

      if (missingletters != "0".u) {
        val reveal: UArrU = ↓(client("open", players("0".u), { () =>
          input("Open it? [Input anything to confirm]")
          ("0".u, new UArrU(Arr.fromSeq(secretWord.map(x => new U8(BigInteger.valueOf(x.asInstanceOf[Int])))), nonce))
        }))
        open2(reveal)
      }

      ()
    }
  }
}
