package prisma

object FLAGS {
  /////////////////////////////////
  // these are compile time flags:

  // - debug builds flags:
  //   create more costly solidity code, with better runtime error messages
  //   impact gas costs
  val GUARD_ERROR_MESSAGES = false // guard build better error messages with string manipulation
  def compiletimeFlagsDescription: String =
    "GUARD_ERROR_MESSAGES=" + GUARD_ERROR_MESSAGES

  // - verbosity flags:
  //   make compile slower
  //   show more info about compiling
  //   will not impact gas cost
  val PRINT_SCALA_INPUT = false
  val PRINT_INTERMEDIATE_COMPILER_PHASES = false
  val PRINT_SCALA_OUTPUT = true
  val PRINT_EVM_BYTECODE = false
  val PRINT_SYMBOLIC_EXECUTION = false
  val PRINT_SYMBOLIC_EXECUTION_STACK = false

  /////////////////////////////////
  // these are runtime time flags:

  val PRINT_DE_SERIALIZATION = false // print serialized, deserilized hex strings
}
