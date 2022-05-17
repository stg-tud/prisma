// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;
pragma experimental "ABIEncoderV2";

// !!! LoC Counted !!!

contract ManualTTT {
    
    address payable[] players;

    uint8 public moves;
    uint8[3][3] board = [[0,0,0],[0,0,0],[0,0,0]];
    
    constructor (address payable[] memory _players) {
        players = _players;
    }

    function move(uint8 x, uint8 y) external  {
        uint8 pos = moves % 2;
        require(moves < 9, "Game has already terinated");
        require(msg.sender == players[pos], "Not the right player to make the move");
        require(board[x][y] == 0, "Invalid move");
        board[x][y] = pos + 2;
        if (board[x][0] == board[x][1] && board[x][1] == board[x][2]
        ||  board[0][y] == board[1][y] && board[1][y] == board[2][y]
        ||  board[1][1] == (pos + 2) && (board[0][2] == board[1][1] && board[1][1] == board[2][0]
                                     ||  board[0][0] == board[1][1] && board[1][1] == board[2][2] )){
            moves = 10 + pos;
	    } else {
	        moves = moves + 1;
	    }
    }
    
    function getBoard() public view returns(uint8[3][3] memory){
        return board;
    }
    
    function getParties() public view returns(address payable[] memory){
        return players;
    }
}
