from collections import deque
import sys
from time import time

# Constants.
BARRIER = -2
EMPTY = -1
HORIZONTAL = 0
VERTICAL = 1
orientationConverter = {0:"HOR",1:"VERT"}

class Player():
    def __init__(self, y:int, x:int, player_id:int, goal_zone:int):
        self.id = player_id
        self.y = (y*2)
        self.x = (x*2)
        self.barriers = 10
        self.goalZone = goal_zone*2
    def getId(self): return self.id
    def getPosition(self): return [self.y, self.x]
    def getGridPosition(self): return [self.y//2, self.x//2]
    def getBarriersRemaining(self): return self.barriers
    def useBarrier(self):self.barriers -= 1
    def addBarrier(self):self.barriers += 1
    def move(self, y, x):
        self.y = y
        self.x = x

class Board():
    def __init__(self, height, width):
        self.height = height
        self.width = width
        self.matrix_height = height*2-1
        self.matrix_width = width*2-1
        self.board = [[EMPTY]*((width*2)-1) for _ in range((height*2)-1)]

    def inBounds(self, i, j):
        return 0<=i and i<self.matrix_height and 0<=j and j<self.matrix_width

    def possibleMoves(self, player: Player):
        possibleMoves = []
        i, j = player.getPosition()

        for di, dj in [[0, 2], [-2, 0], [0, -2], [2, 0]]:
            ni = i + di
            nj = j + dj
            wall_y = i + di // 2
            wall_x = j + dj // 2

            if (self.inBounds(ni, nj) and self.board[wall_y][wall_x] == EMPTY and self.board[ni][nj] == EMPTY):
                possibleMoves.append((ni, nj))

            if self.inBounds(ni, nj) and self.board[ni][nj] != EMPTY:
                jump_y = ni + di
                jump_x = nj + dj
                jump_wall_y = ni + di // 2
                jump_wall_x = nj + dj // 2

                if (self.inBounds(jump_y, jump_x) and self.board[wall_y][wall_x] == EMPTY and self.board[jump_wall_y][jump_wall_x] == EMPTY and self.board[jump_y][jump_x] == EMPTY):
                    possibleMoves.append((jump_y, jump_x))

        for di, dj in [[0, 2], [0, -2], [2, 0], [-2, 0]]:
            opponent_y = i + di
            opponent_x = j + dj

            if not self.inBounds(opponent_y, opponent_x):
                continue
            if self.board[opponent_y][opponent_x] == EMPTY:
                continue

            wall_y = i + di // 2
            wall_x = j + dj // 2
            if self.board[wall_y][wall_x] != EMPTY:
                continue

            behind_y = opponent_y + di
            behind_x = opponent_x + dj
            behind_wall_y = opponent_y + di // 2
            behind_wall_x = opponent_x + dj // 2

            if (self.inBounds(behind_y, behind_x) and self.board[behind_wall_y][behind_wall_x] == EMPTY and self.board[behind_y][behind_x] == EMPTY):
                continue

            if di == 0:
                for diagonal_di in [-2, 2]:
                    ni = opponent_y + diagonal_di
                    nj = opponent_x
                    wall1_y = opponent_y + diagonal_di // 2
                    wall1_x = opponent_x

                    if (self.inBounds(ni, nj) and self.board[wall1_y][wall1_x] == EMPTY and self.board[ni][nj] == EMPTY):
                        possibleMoves.append((ni, nj))
            else:
                for diagonal_dj in [-2, 2]:
                    ni = opponent_y
                    nj = opponent_x + diagonal_dj
                    wall1_y = opponent_y
                    wall1_x = opponent_x + diagonal_dj // 2

                    if (self.inBounds(ni, nj) and self.board[wall1_y][wall1_x] == EMPTY and self.board[ni][nj] == EMPTY):
                        possibleMoves.append((ni, nj))

        return possibleMoves

    def possibleBarriers(self):
        result, seen = [], set()
        for y, x in (p1.getGridPosition(), p2.getGridPosition()):
            for i in range(max(0, y - 5), min(y + 5, self.height - 1)):
                for j in range(max(0, x - 5), min(x + 5, self.width - 1)):
                    for ori in (HORIZONTAL, VERTICAL):
                        if (i, j, ori) not in seen:
                            seen.add((i, j, ori))
                            if self.isPossibleBarrier(i, j, ori):
                                result.append((i, j, ori))
        return result

    def movePlayer(self, player:Player, coordinates: list[int]):
        playerPos = player.getPosition()
        self.board[playerPos[0]][playerPos[1]] = EMPTY
        self.board[coordinates[0]][coordinates[1]] = player.getId()
        player.move(coordinates[0], coordinates[1])

    def canReachGoal(self, player):
        pos = player.getPosition()
        queue = deque([[pos[0], pos[1]]])
        seen = set((pos[0], pos[1]))
        while queue:
            i, j = queue.popleft()
            if i == player.goalZone:
                return True
            for di,dj in [[0,2],[-2,0],[0,-2],[2,0]]:
                wall_y = i+di//2
                wall_x = j+dj//2
                if 0<=i+di and i+di<self.matrix_height and 0<=j+dj and j+dj<self.matrix_width and self.board[wall_y][wall_x] == EMPTY:
                    if (i+di,j+dj) not in seen:
                        queue.append([i+di,j+dj])
                        seen.add((i+di, j+dj))
        return False

    def isValidBarrier(self, i, j, orientation):
        if orientation == VERTICAL:
            wall_y = 2 * i
            wall_x = 2 * j + 1
            return self.board[wall_y][wall_x] == EMPTY and self.board[wall_y + 1][wall_x] == EMPTY and self.board[wall_y + 2][wall_x] == EMPTY
        else:
            wall_y = 2 * i + 1
            wall_x = 2 * j
            return self.board[wall_y][wall_x] == EMPTY and self.board[wall_y][wall_x + 1] == EMPTY and self.board[wall_y][wall_x + 2] == EMPTY

    def isPossibleBarrier(self, i, j, orientation):
        if self.isValidBarrier(i,j,orientation):
            self.changeBarrier(i,j,orientation, BARRIER)
            valid = self.canReachGoal(p1) and self.canReachGoal(p2)
            self.changeBarrier(i,j,orientation, EMPTY)
            return valid
        return False

    def changeBarrier(self, i: int, j: int, orientation: int, key: int):
        wall_y = i*2
        wall_x = j*2
        if orientation == HORIZONTAL:
            wall_y += 1
            self.board[wall_y][wall_x] = key
            self.board[wall_y][wall_x+1] = key
            self.board[wall_y][wall_x+2] = key
        else:
            wall_x += 1
            self.board[wall_y][wall_x] = key
            self.board[wall_y+1][wall_x] = key
            self.board[wall_y+2][wall_x] = key

    def shortestPath(self, player:Player):
        return self.shortestPathFrom(player.y, player.x, player.goalZone)

    def shortestPathFrom(self, i:int, j:int, goalZone:int):
        queue = deque([(i,j, 0)])
        seen = {(i,j)}

        while queue:
            i, j, distance = queue.popleft()

            if i == goalZone:
                return distance

            for di, dj in [[0, 2], [-2, 0], [0, -2], [2, 0]]:
                ni = i + di
                nj = j + dj
                wall_y = i + di // 2
                wall_x = j + dj // 2

                if not self.inBounds(ni, nj):
                    continue
                if self.board[wall_y][wall_x] != EMPTY:
                    continue
                if (ni, nj) not in seen:
                    seen.add((ni, nj))
                    queue.append((ni, nj, distance + 1))

        return float("inf")


def parseAction(action):
    actions = action.split(" ")
    action = actions[0]
    if action == "MOVE":
        return [action, [int(actions[2]) , int(actions[1])]]
    direction = HORIZONTAL if actions[1] == "HOR" else VERTICAL
    return [action, direction, [int(actions[3]) , int(actions[2])]]


def bestAction(me, opponent):
    baseOppDist = board.shortestPath(opponent)
    best = (float("-inf"), None)

    # Candidate moves
    for i, j in board.possibleMoves(me):
        newMyDist = board.shortestPathFrom(i, j, me.goalZone)
        score = baseOppDist - newMyDist
        if score > best[0]:
            best = (score, ("MOVE", (j, i)))

    # Candidate barriers
    if me.getBarriersRemaining() > 0:
        barrierCost = 0.6 if me.getBarriersRemaining() <= 3 else -0.6

        for i, j, ori in board.possibleBarriers():
            board.changeBarrier(i, j, ori, BARRIER)
            newMyDist = board.shortestPath(me)
            newOppDist = board.shortestPath(opponent)
            board.changeBarrier(i, j, ori, EMPTY)

            score = (newOppDist - newMyDist) - barrierCost
            if score > best[0]:
                best = (score, ("BARRIER", (i, j, ori)))

    return best


def err(string):
    print(string, file=sys.stderr)


# Parse inputs and setup basic board.
h, w = map(int, input().split(" "))
my_x, my_y = map(int, input().split(" "))
opp_x, opp_y = map(int, input().split(" "))

board = Board(h,w)
p1 = Player(my_y, my_x, -6, opp_y)
p2 = Player(opp_y, opp_x, -8, my_y)
board.movePlayer(p1, [p1.getPosition()[0], p1.getPosition()[1]])
board.movePlayer(p2, [p2.getPosition()[0], p2.getPosition()[1]])

# Gameloop
while True:
    start=time()
    opp_action = input()
    if opp_action != "NONE":
        action = parseAction(opp_action)
        if action[0] == "MOVE":
            board.movePlayer(p2, [i*2 for i in action[1]])
        else:
            board.changeBarrier(*action[2], action[1], BARRIER)
            p2.useBarrier()

    score, move = bestAction(p1, p2)
    if move[0] == "MOVE":
        print(f"MOVE {(move[1][0])//2} {(move[1][1])//2}")
        board.movePlayer(p1, [move[1][1], move[1][0]])
    else:
        print(f"BARRIER {orientationConverter[move[1][2]]} {move[1][1]} {move[1][0]}")
        board.changeBarrier(move[1][0], move[1][1], move[1][2], BARRIER)
        p1.useBarrier()
    err(f"{(time()-start)*100} ms")