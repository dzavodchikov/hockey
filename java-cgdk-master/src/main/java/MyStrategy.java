import model.*;

import static java.lang.StrictMath.min;

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
                        double angle = getAngleToFarNet(self, opponent);
                        move.setSpeedUp(1.0D);
                        move.setTurn(angle);
                        move.setAction(ActionType.NONE);
                    }
                } else {
                    Position forward = getForwardPosition(self, world);
                    double angle = self.getAngleTo(forward.getX(), forward.getY());
                    move.setSpeedUp(1.0D);
                    move.setTurn(angle);
                    move.setAction(ActionType.NONE);
                }
            } else {
                if (isHockeyistCanTakePuck(self, game, puck)) {
                    double angle = self.getAngleTo(world.getPuck());
                    move.setSpeedUp(1.0D);
                    move.setTurn(angle);
                    move.setAction(ActionType.TAKE_PUCK);
                } else {
                    double angle = self.getAngleTo(puck);
                    move.setSpeedUp(1.0D);
                    move.setTurn(angle);
                    move.setAction(ActionType.NONE);
                }
            }
        }

        if (isHockeyistDefender(self)) {
            if (isPlayerControlPuck(puck, myPlayer)) {
                if (isHockeyistControlPuck(puck, self)) {
                    if (isHockeyistNearToOpponentNet(self, opponent)) {
                        move.setAction(ActionType.STRIKE);
                    } else {
                        Hockeyist forward = getForwardHockeyist(world);
                        if (isHockeyistOpen(forward, world, game)) {
                            if (canTakePass(self, forward, game)) {
                                double angle = self.getAngleTo(forward);
                                move.setPassPower(1.0D);
                                move.setPassAngle(angle);
                                move.setAction(ActionType.PASS);
                            } else {
                                double angle = self.getAngleTo(forward.getX(), forward.getY());
                                move.setSpeedUp(1.0D);
                                move.setTurn(angle);
                                move.setAction(ActionType.NONE);
                            }
                        } else {
                            double angle = getAngleToFarNet(self, opponent);
                            move.setSpeedUp(1.0D);
                            move.setTurn(angle);
                            move.setAction(ActionType.NONE);
                        }
                    }
                } else {
                    Position back = getDefenderPosition(world);
                    double angle = self.getAngleTo(back.getX(), back.getY());
                    move.setSpeedUp(1.0D);
                    move.setTurn(angle);
                    move.setAction(ActionType.NONE);
                }
            } else {
                if (isHockeyistCanTakePuck(self, game, puck)) {
                    double angle = self.getAngleTo(world.getPuck());
                    move.setSpeedUp(1.0D);
                    move.setTurn(angle);
                    move.setAction(ActionType.TAKE_PUCK);
                } else {
                    if (isPlayerControlPuck(puck, opponent)) {
                        double angle = getInterceptDirection(self, world);
                        move.setSpeedUp(1.0D);
                        move.setTurn(angle);
                        move.setAction(ActionType.NONE);
                    } else {
                        double angle = self.getAngleTo(world.getPuck());
                        move.setSpeedUp(1.0D);
                        move.setTurn(angle);
                        move.setAction(ActionType.NONE);
                    }
                }
            }
        }



    }

    private boolean canTakePass(Hockeyist self, Hockeyist forward, Game game) {
        if (self.getAngleTo(forward) < game.getPassSector() / 4) {
            return true;
        } else {
            return false;
        }
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

    private Hockeyist getForwardHockeyist(World world) {
        Hockeyist forward = null;
        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() && isHockeyistForward(hockeyist)) {
                forward = hockeyist;
            }
        }
        return forward;
    }

    private boolean isHockeyistControlPuck(Puck puck, Hockeyist self) {
        if (self.getId() == puck.getOwnerHockeyistId()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isHockeyistNearToOpponentNet(Hockeyist self, Player opponent) {
        double angle = getAngleToFarNet(self, opponent);
        Position netCenter = getNetCenter(opponent);
        double distance = self.getDistanceTo(netCenter.getX(), netCenter.getY());
        return Math.abs(angle) < 0.1 && distance < 300;
    }

    private boolean isHockeyistDefender(Hockeyist self) {
        return self.getOriginalPositionIndex() == 1;
    }

    private boolean isHockeyistForward(Hockeyist self) {
        return self.getOriginalPositionIndex() == 0;
    }

    private boolean isHockeyistCanTakePuck(Hockeyist self, Game game, Puck puck) {
        return self.getDistanceTo(puck) < game.getStickLength();
    }

    private boolean isPlayerControlPuck(Puck puck, Player player) {
        return player.getId() == puck.getOwnerPlayerId();
    }

    private double getInterceptDirection(Hockeyist hockeyist, World world) {

        Hockeyist goalie = getMyGoalie(world);
        Hockeyist opponent = getNearestOpponentToMyNet(world);

        double x = (goalie.getX() + opponent.getX()) / 2;
        double y = (goalie.getY() + opponent.getY()) / 2;

        return hockeyist.getAngleTo(x, y);

    }

    private Hockeyist getNearestOpponentToMyNet(World world) {

        Position netCenter = getNetCenter(world.getMyPlayer());
        Hockeyist nearestOpponent = null;

        double minDistance = Double.MAX_VALUE;

        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() == false) {
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
            if (hockeyist.isTeammate() == true && hockeyist.getType() == HockeyistType.GOALIE) {
                return hockeyist;
            }
        }
        return null;
    }

    private Position getNetCenter(Player player) {
        return new Position(player.getNetFront(), (player.getNetTop() + player.getNetBottom()) / 2);
    }

    private Position getDefenderPosition(World world) {
        Hockeyist hockeyist = getNearestOpponentToMyNet(world);
        Position netCenter = getNetCenter(world.getMyPlayer());
        if (netCenter.getX() < hockeyist.getX()) {
            return new Position(hockeyist.getX() - 100, netCenter.getY());
        } else {
            return new Position(hockeyist.getX() + 100, netCenter.getY());
        }
    }

    private Position getForwardPosition(Hockeyist self, World world) {
        Position netCenter = getNetCenter(world.getOpponentPlayer());
        if (netCenter.getX() < self.getX()) {
            return new Position(netCenter.getX() + 300, netCenter.getY() - 150);
        } else {
            return new Position(netCenter.getX() - 300, netCenter.getY() + 150);
        }
    }

    private double getAngleToNearNet(Hockeyist hockeyist, Player player) {
        if (hockeyist.getY() > (player.getNetTop() + player.getNetBottom()) / 2) {
            return hockeyist.getAngleTo(player.getNetFront(), player.getNetBottom() - 20);
        } else {
            return hockeyist.getAngleTo(player.getNetFront(), player.getNetTop() + 20);
        }
    }

    private double getAngleToFarNet(Hockeyist hockeyist, Player player) {
        if (hockeyist.getY() > (player.getNetTop() + player.getNetBottom()) / 2) {
            return hockeyist.getAngleTo(player.getNetFront(), player.getNetTop() + 20);
        } else {
            return hockeyist.getAngleTo(player.getNetFront(), player.getNetBottom() - 20);
        }
    }

}
