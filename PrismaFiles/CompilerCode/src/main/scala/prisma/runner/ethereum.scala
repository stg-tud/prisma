package prisma.runner

object ganache {
  val defaultAccount1 = new Account("0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d")
  val defaultAccount2 = new Account("0x6cbed15c793ce57650b9877cf6fa156fbef513c4e6134f022a85b1ffdd59b2a1")
  val defaultAccount3 = new Account("0x6370fd033278c143179d81c5526140625662b8daa446c22ee2d73db3707e620c")
  val defaultAccount4 = new Account("0x646f1ce2fdad0e6deeeb5c7e8e5543bdde65e86029e2fd9fc169899c440a7913")
}

object ethereum {

  // evm opcodes, see:
  // * yellow paper
  // * https://github.com/crytic/pyevmasm/blob/master/pyevmasm/evmasm.py
  // * https://github.com/ethereum/go-ethereum/blob/master/core/vm/opcodes.go
  // * https://github.com/ethereum/solidity/blob/develop/libevmasm/Instruction.h
  // * https://ethervm.io/

  def isPush(inst: Int): Boolean = 0x60 <= inst && inst < 0x80 // 0x60 is the first and 0x7f is the last a push instruction
  def isDup(inst: Int): Boolean = 0x80 <= inst && inst < 0x90
  def isSwap(inst: Int): Boolean = 0x90 <= inst && inst < 0xa0
  def pushLength(inst: Int): Int = inst - (0x60 - 1)
  def dupDepth(inst: Int): Int = inst - (0x80 - 1)
  def swapDepth(inst: Int): Int = inst - (0x90 - 1)
  def instrLength(inst: Int): Int = 1 + (if (isPush(inst)) pushLength(inst) else 0)
  def opcodeName(i: Int, default: String): String = opcodes.get(i).map(x => x._4).getOrElse(default)

  val opcodes = Map(
    // hex -> pop, push, gas, name
    0x00 -> ((0,0,0, "STOP")),
    0x01 -> ((2,1,3, "ADD")),
    0x02 -> ((2,1,5, "MUL")),
    0x03 -> ((2,1,3, "SUB")),
    0x04 -> ((2,1,5, "DIV")),
    0x05 -> ((2,1,5, "SDIV")),
    0x06 -> ((2,1,5, "MOD")),
    0x07 -> ((2,1,5, "SMOD")),
    0x08 -> ((3,1,8, "ADDMOD")),
    0x09 -> ((3,1,8, "MULMOD")),
    0x0a -> ((2,1,10, "EXP")),
    0x0b -> ((2,1,5, "SIGNEXTEND")),

    0x10 -> ((2,1,3, "LT")),
    0x11 -> ((2,1,3, "GT")),
    0x12 -> ((2,1,3, "SLT")),
    0x13 -> ((2,1,3, "SGT")),
    0x14 -> ((2,1,3, "EQ")),
    0x15 -> ((1,1,3, "ISZERO")),
    0x16 -> ((2,1,3, "AND")),
    0x17 -> ((2,1,3, "OR")),
    0x18 -> ((2,1,3, "XOR")),
    0x19 -> ((1,1,3, "NOT")),
    0x1a -> ((2,1,3, "BYTE")),
    0x1b -> ((2,1,3, "SHL")), // constantinople
    0x1c -> ((2,1,3, "SHR")), // constantinople
    0x1d -> ((2,1,3, "SAR")), // constantinople

    0x20 -> ((2,1,30, "SHA3")),

    0x30 -> ((0,1,2, "ADDRESS")),
    0x31 -> ((1,1,20, "BALANCE")),
    0x32 -> ((0,1,2, "ORIGIN")),
    0x33 -> ((0,1,2, "CALLER")),
    0x34 -> ((0,1,2, "CALLVALUE")),
    0x35 -> ((1,1,3, "CALLDATALOAD")),
    0x36 -> ((0,1,2, "CALLDATASIZE")),
    0x37 -> ((3,0,3, "CALLDATACOPY")),
    0x38 -> ((0,1,2, "CODESIZE")),
    0x39 -> ((3,0,3, "CODECOPY")),
    0x3a -> ((0,1,2, "GASPRICE")),
    0x3b -> ((1,1,20, "EXTCODESIZE")),
    0x3c -> ((4,0,20, "EXTCODECOPY")),
    //(0x3d, "RETURNDATASIZE")),
    //(0x3e, "RETURNDATACOPY")),

    0x40 -> ((1,1,20, "BLOCKHASH")),
    0x41 -> ((0,1,2, "COINBASE")),
    0x42 -> ((0,1,2, "TIMESTAMP")),
    0x43 -> ((0,1,2, "NUMBER")),
    0x44 -> ((0,1,2, "DIFFICULTY")),
    0x45 -> ((0,1,2, "GASLIMIT")),
    //(0x48, "NONCE")), // *
    //(0x49, "PAYGAS")), // *

    0x50 -> ((1,0,1, "POP")),
    0x51 -> ((1,1,3, "MLOAD")),
    0x52 -> ((2,0,3, "MSTORE")),
    0x53 -> ((2,0,3, "MSTORE8")),
    0x54 -> ((1,1,50, "SLOAD")),
    0x55 -> ((2,0,0, "SSTORE")), // 5_000 vs 20_000 vs -15_000
    0x56 -> ((1,0,8, "JUMP")),
    0x57 -> ((2,0,10, "JUMPI")),
    0x58 -> ((1,1,2, "GETPC")),
    0x59 -> ((0,1,2, "MSIZE")),
    0x5a -> ((0,1,2, "GAS")),
    0x5b -> ((0,0,1, "JUMPDEST")),

    0x60 -> ((0,1,3, "PUSH1")),
    0x61 -> ((0,1,3, "PUSH2")),
    0x62 -> ((0,1,3, "PUSH3")),
    0x63 -> ((0,1,3, "PUSH4")),
    0x64 -> ((0,1,3, "PUSH5")),
    0x65 -> ((0,1,3, "PUSH6")),
    0x66 -> ((0,1,3, "PUSH7")),
    0x67 -> ((0,1,3, "PUSH8")),
    0x68 -> ((0,1,3, "PUSH9")),
    0x69 -> ((0,1,3, "PUSH10")),
    0x6a -> ((0,1,3, "PUSH11")),
    0x6b -> ((0,1,3, "PUSH12")),
    0x6c -> ((0,1,3, "PUSH13")),
    0x6d -> ((0,1,3, "PUSH14")),
    0x6e -> ((0,1,3, "PUSH15")),
    0x6f -> ((0,1,3, "PUSH16")),

    0x70 -> ((0,1,3, "PUSH17")),
    0x71 -> ((0,1,3, "PUSH18")),
    0x72 -> ((0,1,3, "PUSH19")),
    0x73 -> ((0,1,3, "PUSH20")),
    0x74 -> ((0,1,3, "PUSH21")),
    0x75 -> ((0,1,3, "PUSH22")),
    0x76 -> ((0,1,3, "PUSH23")),
    0x77 -> ((0,1,3, "PUSH24")),
    0x78 -> ((0,1,3, "PUSH25")),
    0x79 -> ((0,1,3, "PUSH26")),
    0x7a -> ((0,1,3, "PUSH27")),
    0x7b -> ((0,1,3, "PUSH28")),
    0x7c -> ((0,1,3, "PUSH29")),
    0x7d -> ((0,1,3, "PUSH30")),
    0x7e -> ((0,1,3, "PUSH31")),
    0x7f -> ((0,1,3, "PUSH32")),

    0x80 -> ((1,2,3, "DUP1")),
    0x81 -> ((2,3,3,"DUP2")),
    0x82 -> ((3,4,3, "DUP3")),
    0x83 -> ((4,5,3, "DUP4")),
    0x84 -> ((5,6,3, "DUP5")),
    0x85 -> ((6,7,3, "DUP6")),
    0x86 -> ((7,8,3, "DUP7")),
    0x87 -> ((8,9,3, "DUP8")),
    0x88 -> ((9,10,3, "DUP9")),
    0x89 -> ((10,11,3, "DUP10")),
    0x8a -> ((11,12,3, "DUP11")),
    0x8b -> ((12,13,3, "DUP12")),
    0x8c -> ((13,14,3, "DUP13")),
    0x8d -> ((14,15,3, "DUP14")),
    0x8e -> ((15,16,3, "DUP15")),
    0x8f -> ((16,17,3, "DUP16")),

    0x90 -> ((2,2,3, "SWAP1")),
    0x91 -> ((3,3,3, "SWAP2")),
    0x92 -> ((4,4,3, "SWAP3")),
    0x93 -> ((5,5,3, "SWAP4")),
    0x94 -> ((6,6,3, "SWAP5")),
    0x95 -> ((7,7,3, "SWAP6")),
    0x96 -> ((8,8,3, "SWAP7")),
    0x97 -> ((9,9,3, "SWAP8")),
    0x98 -> ((10,10,3, "SWAP9")),
    0x99 -> ((11,11,3, "SWAP10")),
    0x9a -> ((12,12,3, "SWAP11")),
    0x9b -> ((13,13,3, "SWAP12")),
    0x9c -> ((14,14,3, "SWAP13")),
    0x9d -> ((15,15,3, "SWAP14")),
    0x9e -> ((16,16,3, "SWAP15")),
    0x9f -> ((17,17,3, "SWAP16")),

    0xa0 -> ((2,0,375, "LOG0")),
    0xa1 -> ((3,0,750, "LOG1")),
    0xa2 -> ((4,0,1125, "LOG2")),
    0xa3 -> ((5,0,1500, "LOG3")),
    0xa4 -> ((6,0,1875, "LOG4")),

    0xf0 -> ((3,1,32000, "CREATE")),
    0xf1 -> ((7,1,40, "CALL")),
    0xf2 -> ((7,1,40, "CALLCODE")),
    0xf3 -> ((2,0,0, "RETURN")),
    0xf4 -> ((0,0,0, "DELEGATECALL")),
    //(0xf5, "BREAKPOINT")),
    //(0xf6, "RNGSEED")),
    //(0xf7, "SSIZEEXT")),
    //(0xf8, "SLOADBYTES")),
    //(0xf9, "SSTOREBYTES")),
    //(0xfa, "SSIZE")),
    //(0xfb, "STATEROOT")),
    //(0xfc, "TXEXECGAS")),
    0xfd -> ((2,0,0, "REVERT")), // byzantinum
    0xfe -> ((0,0,0, "INVALID")),
    0xff -> ((1,0,0, "SELFDESTRUCT")),
  )

}
