// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0;

contract ManualChineseCheckers {

    address[] parties;
    //Internally positions have coordinates (d,l1,l2) in which d (0-5) describes the direction to got from the middle, l1 (1-5) describes the steps to go from the middle and l2 (0-5) the steps to go after taking a 60 degree turn to the right; the middle is 0-0-0
    uint8[121] public positions = [0,0,0,0,1,1,0,0,1,1,1,0,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,2,2,0,0,2,2,2,0,2,2,2,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,3,3,0,0,3,3,3,0,3,3,3,3,3,3,3,3,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3,0,0,0,0];
    uint public moves = 0;
    
    function getPositions() public view returns(uint8[121] memory){
        return positions;
    }
    
    function getParties() public view returns(address[] memory){
    	return parties;
    }
    
    
    constructor(address[] memory _parties) public {
        parties = _parties;
    }
    
    function move(uint _rootPos, uint8[] memory _directions) public isTurn {
        
         uint8 _playerIndex = uint8(moves % 3) + 1;
        
        //Checks: Not done and right starting position
        require(moves < 5000, "Game already done");
        require(positions[_rootPos] == _playerIndex, "Not the players token");
        
        //Execute move
        uint _pos = _rootPos;
        bool _leaped = false;
        
        for(uint i = 0; i < _directions.length; i++){
            
            //Get target for every hop 
            (_pos, _leaped) = testStep(_pos, _directions[i]);
             
            //Revert if target does not exist
            if(_pos == 122){
                revert("Moved out of the board");
            }
            
            //Revert if no leap over other figure but move contains more than one hop
            if(_leaped == false && _directions.length != 1){
                revert("No leap but multiple moves");
            }
        }
        
        //Execute move
        positions[_rootPos] = 0;
        positions[_pos] = _playerIndex;
        
        //Check if the player wins
        if(goalCheck(_playerIndex)){
	        moves = 5000 + _playerIndex;
	    } else {
	        moves++;
	    }
    }
	
	//Checks if a single player wins
	function goalCheck(uint _playerID) internal view returns(bool){
	    uint checkDirection;
	    if(_playerID == 1){
	        checkDirection = 3;
	    } else if(_playerID == 2){
	        checkDirection = 5;
	    } else if(_playerID == 3){
	        checkDirection = 1;
	    } else {
	        revert("Wrong player id in goal check");
	    }
	    
	    //Check, if every field in player's target corner is occupied by the player:
	    //  First directions runs down from 4 to 1
	    //  Second direction runs down from 4 to (4 -first direction)
	    for(uint b = 4; b >= 1; b--){
	        for(uint d = 5-b-1; d <= 4; d++){ //uint cant get below 0, therefore # d >= (4-b); d--# would be endless
	            if(positions[coordinatesToIndex(checkDirection,b,d)] != _playerID){
	                return false;
	            }
	        }
	    }
	    
	    //The corner-stone of the next triangle needs to be checked as well:
	    if(positions[coordinatesToIndex(((checkDirection+1)%6),4,0)] != _playerID){
	        return false;
	    }
	    
		return true;
	}
    
    //Converts a coordinate from the calculation-representation to the internal representation (index in position-array)
	//Checks if coordinate exists and returns 122 in robust mode or aborts in non-robust mode, if coordinate does not exist
	function coordinatesToIndexChecked(uint _dir, uint _length1, uint _length2) internal pure returns (uint){
	     //Check if position exists:
	    if(_dir > 5 || _length1 == 0 || _length1 > 4 || _length2 > 4){
	        revert("Jump to invalid coordinate");
	    }
	    return coordinatesToIndex(_dir, _length1, _length2);
	}
	
	//Converts a coordinate from the calculation-representation to the internal representation (index in position-array)
	function coordinatesToIndex(uint _dir, uint _length1, uint _length2) internal pure returns (uint){
	     //Check if position exists:
	    return _dir * 20 + (_length1-1) * 5 + (_length2 + 1);
	}
    
    
    //Tests if step is possible and returns new position or 122 if move is not possible
    //Additionally returns if hop leaped over intermediary
    function testStep(uint _root, uint8 _direction) public view returns(uint,bool){
        
        //Return variables
        uint _newPos;
        bool _leaped = false;
        
        //First Hop
        _newPos = getHopTarget( _root, _direction);
        
        //If target exists and not empty: leap
        if(_newPos != 122 && positions[_newPos] != 0){
            
            //Leap over target
            _newPos = getHopTarget(_newPos, _direction);
            
            //If field exists and is not empty -> return 122
            if(_newPos != 122 && positions[_newPos]  != 0){
                return ( 122, false);
            }
            
            //Set leaped to true if valid leaping hop
            _leaped = true;
        }
        return (_newPos,_leaped);
    }
    
    //Returns id of new position or 122 if position does not exist
	function getHopTarget(uint _pre, uint _direction) public pure returns (uint){
        
        //Convert position in array to coordinate
        (uint _preDirection, uint _preLength1, uint _preLength2) = indexToCoordinate(_pre);
        
	    //Check if direction is right
	    if(_direction > 5){return 122;}
	    //Check if position is center
	    if(_preDirection + _preLength1 + _preLength2 == 0){ return coordinatesToIndex(_direction,1,0);}
	    //Check if direction is 0 degree from base direction
	    if(_direction == _preDirection){
	        // (x,y+1,z)
	        return coordinatesToIndexChecked(_preDirection, (_preLength1 + 1), _preLength2);
	    }
	    //Check if direction is 60 degree from base direction
	    if(_direction == ((_preDirection+1)%6)){
	        // (x,y,z+1)
	        return coordinatesToIndexChecked(_preDirection , _preLength1 , _preLength2 + 1);
	    }
	    //Check if direction is 120 degree from base direction
	    if(_direction == ((_preDirection+2)%6)){
	        if(_preLength1 == 1){
	            //New base direction: (x+1%6,z,0)
	            return coordinatesToIndexChecked(((_preDirection+1)%6) , (_preLength2 + 1) , 0);
	        } else {
	            //Same base direction (x,y-1,z+1)
	            return coordinatesToIndexChecked(_preDirection , (_preLength1 - 1) , _preLength2 + 1);
	        }
	    }
	    //Check if direction is 180 degree from base direction
	    if(_direction == ((_preDirection+3)%6)){
	        if(_preLength1 == 1){
	            if(_preLength2 == 0){
	                //Go to center: 0
	                return 0;
	            } else {
	                //New base direction: (x+1%6,z,0)
	                return coordinatesToIndex(((_preDirection+1)%6), _preLength2, 0);
	            }
	        } else {
	            //Same base direction (x,y-1,z)
	            return coordinatesToIndex(_preDirection, (_preLength1 - 1), _preLength2);
	        }
	    }
	    //Check if direction is 240 degree from base direction
	    if(_direction == ((_preDirection+4)%6)){
	        if(_preLength2 == 0){
	            //New base direction: (x-1%6, 1, y-1)
	            return coordinatesToIndex(((_preDirection+5)%6),1,_preLength1 - 1);
	        } else {
	            //Same base direction: (x,y,z-1)
	            return coordinatesToIndex(_preDirection, _preLength1, _preLength2 - 1);
	        }
	    }
	    //Check if direction is 300 degree from base direction
	    if(_direction == ((_preDirection+5)%6)){
	        if(_preLength2 == 0){
	            //New base direction: (x-1%6, 1, y)
	            return coordinatesToIndex(((_preDirection+5)%6),1, _preLength1);
	        } else {
	            //Same base direction: (x,y+1,z-1)
	            return coordinatesToIndex(_preDirection,(_preLength1 + 1), _preLength2 - 1);
	        }
	    }
	    
	    //Should not occur
	    revert ("Hop with wrong dimension");
	}
    
    //Converts internal position (index) into coordinate
	function indexToCoordinate(uint _index) public pure returns (uint, uint, uint){
	    
	    require(_index < 121);
	    
	    if(_index == 0){
	        return (0,0,0);
	    }
	    
	    uint _length2 = (_index - 1) % 5;
	    uint _length1 = (((_index - 1 - _length2) / 5 ) % 4) + 1;
	    uint _dir = ((_index - 1) - ((_index - 1) % 20)) / 20;
	    return (_dir, _length1, _length2);
	}
	
	modifier isTurn() {
        require(msg.sender == parties[moves % 3], "Not the right player to make a move");
        _;
    }
}
