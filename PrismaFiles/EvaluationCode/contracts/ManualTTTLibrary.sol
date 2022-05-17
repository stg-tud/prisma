// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;
pragma experimental "ABIEncoderV2";

contract ManualTTTLibrary {
    
    function checkWin(uint8[][] memory board, uint8 x, uint8 y) public pure {
        
        if (board[x][0] == board[x][1] && board[x][1] == board[x][2]
        ||  board[0][y] == board[1][y] && board[1][y] == board[2][y]
        || board[1][1] != 0 && (
        	  board[0][2] == board[1][1] && board[1][1] == board[2][0]
        	  ||  board[0][0] == board[1][1] && board[1][1] == board[2][2] )){
            require(true);
        } else {
            require(false);
        }
        
    }
    
//    function testSignature(uint8[][] memory board, uint8 x, uint8 y) public pure returns (bytes memory){
//        return abi.encode(bytes4(keccak256("checkWin(uint8[][],uint8,uint8)")), board, x, y);
//    }
    
//    function testSignatureTwo(uint8[][] memory board, uint8 x, uint8 y) public pure returns (bytes memory){
//        return abi.encodePacked("\xca\x76\x1a\x21", abi.encode(board, x, y));
//    }
}
