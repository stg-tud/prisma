// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;
pragma experimental "ABIEncoderV2";

// !!! LoC Counted !!!

contract ManualTTTViaLib {
    
    address payable[] players;

    uint8 public moves;
    uint8[][] board = [[0,0,0],[0,0,0],[0,0,0]];
    
    constructor (address payable[] memory _players) {
        players = _players;
    }

    function move(uint8 x, uint8 y) external  {
        uint8 pos = moves % 2;
        require(moves < 9, "Game has already terinated");
        require(msg.sender == players[pos], "Not the right player to make the move");
        require(board[x][y] == 0, "Invalid move");
        board[x][y] = pos + 2;
        (bool win,) = 0xCfEB869F69431e42cdB54A4F4f105C19C080A601.call(abi.encodePacked("\xca\x76\x1a\x21", abi.encode(board, x, y)));
        if (win){
	    moves = 10 + pos;
	    } else {
		moves = moves + 1;
	    }
    }
    
    function getBoard() public view returns(uint8[][] memory){
        return board;
    }
    
    function getParties() public view returns(address payable[] memory){
        return players;
    }
}
