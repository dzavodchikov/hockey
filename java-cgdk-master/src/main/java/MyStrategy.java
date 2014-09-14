import model.*;

public final class MyStrategy implements Strategy {

    @Override
    public void move(Hockeyist self, World world, Game game, Move move) {

        Puck puck = world.getPuck();
        Player myPlayer = world.getMyPlayer();
        Player opponent = world.getOpponentPlayer();

        if (isHockeyistForward(self)) {
            if (isPlayerControlPuck(puck, myPlayer)) {
                if (isHockeyistControlPuck(puck, self)) {
                    if (isHockeyistNearToOpponentNet(self, opponent)) {
                        move.setAction(ActionType.STRIKE);
                    } else {
                        Position farNet = getFarNet(self, opponent);
                        moveHockeyistTo(self, move, farNet, game);
                    }
                } else {
                    Position forwardPosition = getForwardPosition(self, world);
                    moveHockeyistTo(self, move, forwardPosition, game);
                }
            } else {
                if (isHockeyistCanTakePuck(self, game, puck)) {
                    move.setAction(ActionType.TAKE_PUCK);
                } else {
                    Position futurePuckPosition = getFuturePuckPosition(self, world, game);
                    moveHockeyistTo(self, move, futurePuckPosition, game);
                }
            }
        }

        if (isHockeyistDefender(self)) {
            if (isPlayerControlPuck(puck, myPlayer)) {
                if (isHockeyistControlPuck(puck, self)) {
                    if (isHockeyistNearToOpponentNet(self, opponent)) {
                        move.setAction(ActionType.STRIKE);
                    } else {
                        Hockeyist forward = getMyForward(world);
                        Position opponentNet = getNetCenter(world.getOpponentPlayer());
                        if (isHockeyistOpen(forward, world, game)) {
                            if (isHockeyistCanTakePass(self, forward, game) &&
                                    self.getDistanceTo(opponentNet.getX(), opponentNet.getY()) >
                                            forward.getDistanceTo(opponentNet.getX(), opponentNet.getY())) {
                                move.setAction(ActionType.PASS);
                            } else {
                                moveHockeyistTo(self, move, forward, game);
                            }
                        } else {
                            Position farNet = getFarNet(self, opponent);
                            moveHockeyistTo(self, move, farNet, game);
                        }
                    }
                } else {
                    Position back = getDefenderPosition(world);
                    moveHockeyistTo(self, move, back, game);
                }
            } else {
                if (isHockeyistCanTakePuck(self, game, puck)) {
                    move.setAction(ActionType.TAKE_PUCK);
                } else {
                    if (isPlayerControlPuck(puck, opponent)) {
                        Position interceptPosition = getInterceptPosition(self, world);
                        moveHockeyistTo(self, move, interceptPosition, game);
                    } else {
                        Position futurePuckPosition = getFuturePuckPosition(self, world, game);
                        moveHockeyistTo(self, move, futurePuckPosition, game);
                    }
                }
            }
        }



    }

    private Position getFuturePuckPosition(Hockeyist self, World world, Game game) {
        Puck puck = world.getPuck();
        double s = self.getDistanceTo(puck);
        double v = (game.getHockeyistMaxSpeed() + getSpeed(self)) / 2;
        double t = s / v;
        return new Position(puck.getX() + puck.getSpeedX() * t, puck.getY() + puck.getSpeedY() * t);
    }

    private double getSpeed(Unit self) {
        return Math.sqrt(self.getSpeedX() * self.getSpeedX() + self.getSpeedY() * self.getSpeedY());
    }

    private void moveHockeyistTo(Hockeyist self, Move move, Unit unit, Game game) {
        moveHockeyistTo(self, move, unit.getX(), unit.getY(), game);
    }

    private void moveHockeyistTo(Hockeyist self, Move move, Position position, Game game) {
        moveHockeyistTo(self, move, position.getX(), position.getY(), game);
    }

    private void moveHockeyistTo(Hockeyist self, Move move, double x, double y, Game game) {
        double angle = self.getAngleTo(x, y);
        double distance = self.getDistanceTo(x, y);
        double speed = getSpeed(self);
        double a = game.getHockeyistSpeedDownFactor();
        if (distance > (speed * speed)/(2 * a)) {
            move.setSpeedUp(1.0D);
        } else {
            move.setSpeedUp(-1.0D);
        }
        move.setTurn(angle);
        move.setAction(ActionType.NONE);
    }

    private boolean isHockeyistCanTakePass(Hockeyist self, Hockeyist forward, Game game) {
        return self.getRemainingCooldownTicks() == 0
                && self.getAngleTo(forward) < game.getPassSector() / 4;
    }

    private boolean isHockeyistOpen(Hockeyist forward, World world, Game game) {
        boolean open = true;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.getPlayerId() != forward.getPlayerId() &&
                    forward.getDistanceTo(hockeyist) < game.getStickLength()) {
                open = false;
            }
        }
        return open;
    }

    private Hockeyist getMyForward(World world) {
        Hockeyist forward = null;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() && isHockeyistForward(hockeyist)) {
                forward = hockeyist;
            }
        }
        return forward;
    }

    private Hockeyist getMyDefender(World world) {
        Hockeyist defander = null;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() && isHockeyistDefender(hockeyist)) {
                defander = hockeyist;
            }
        }
        return defander;
    }

    private boolean isHockeyistControlPuck(Puck puck, Hockeyist self) {
        return self.getId() == puck.getOwnerHockeyistId();
    }

    private boolean isHockeyistNearToOpponentNet(Hockeyist self, Player opponent) {
        Position farNet = getFarNet(self, opponent);
        Position netCenter = getNetCenter(opponent);
        double distance = self.getDistanceTo(netCenter.getX(), netCenter.getY());
        return Math.abs(self.getAngleTo(farNet.getX(), farNet.getY())) < 0.1 && distance < 300;
    }

    private boolean isHockeyistDefender(Hockeyist self) {
        return self.getOriginalPositionIndex() == 1;
    }

    private boolean isHockeyistForward(Hockeyist self) {
        return self.getOriginalPositionIndex() == 0;
    }

    private boolean isHockeyistCanTakePuck(Hockeyist self, Game game, Puck puck) {
        return self.getRemainingCooldownTicks() == 0
                && self.getDistanceTo(puck) < game.getStickLength()
                && Math.abs(self.getAngleTo(puck)) < game.getStickSector() / 2;
    }

    private boolean isPlayerControlPuck(Puck puck, Player player) {
        return player.getId() == puck.getOwnerPlayerId();
    }

    private Position getInterceptPosition(Hockeyist hockeyist, World world) {
        Position netCenter = getNetCenter(world.getMyPlayer());
        Hockeyist opponent = getNearestOpponentToMyNet(world);
        double x = (netCenter.getX() + opponent.getX()) / 2;
        double y = (netCenter.getY() + opponent.getY()) / 2;
        return new Position(x, y);
    }

    private Hockeyist getNearestOpponentToMyNet(World world) {

        Position netCenter = getNetCenter(world.getMyPlayer());
        Hockeyist nearestOpponent = null;

        double minDistance = Double.MAX_VALUE;

        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (!hockeyist.isTeammate()) {
                double distance = hockeyist.getDistanceTo(netCenter.getX(), netCenter.getY());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestOpponent = hockeyist;
                }
            }
        }

        return  nearestOpponent;

    }

    private Hockeyist getMyGoalie(World world) {
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() && hockeyist.getType() == HockeyistType.GOALIE) {
                return hockeyist;
            }
        }
        return null;
    }

    private Position getNetCenter(Player player) {
        return new Position(player.getNetFront(), (player.getNetTop() + player.getNetBottom()) / 2);
    }

    private Position getDefenderPosition(World world) {
        Hockeyist nearestOpponent = getNearestOpponentToMyNet(world);
        Position netCenter = getNetCenter(world.getMyPlayer());
        if (netCenter.getX() < nearestOpponent.getX()) {
            return new Position(nearestOpponent.getX() - 150, netCenter.getY());
        } else {
            return new Position(nearestOpponent.getX() + 150, netCenter.getY());
        }
    }

    private Position getForwardPosition(Hockeyist self, World world) {
        Position opponentNetCenter = getNetCenter(world.getOpponentPlayer());
        if (opponentNetCenter.getX() < self.getX()) {
            return new Position(opponentNetCenter.getX() + 300, opponentNetCenter.getY() - 150);
        } else {
            return new Position(opponentNetCenter.getX() - 300, opponentNetCenter.getY() + 150);
        }
    }

    private Position getNearNet(Hockeyist hockeyist, Player player) {
        if (hockeyist.getY() > (player.getNetTop() + player.getNetBottom()) / 2) {
            return new Position(player.getNetFront(), player.getNetBottom() - 20);
        } else {
            return new Position(player.getNetFront(), player.getNetTop() + 20);
        }
    }

    private Position getFarNet(Hockeyist hockeyist, Player player) {
        if (hockeyist.getY() > (player.getNetTop() + player.getNetBottom()) / 2) {
            return new Position(player.getNetFront(), player.getNetTop() + 20);
        } else {
            return new Position(player.getNetFront(), player.getNetBottom() - 20);
        }
    }

}
