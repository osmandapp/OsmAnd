:- op('==', xfy, 500).
version(0).
% provided by GaudiumSoft Lukasz Lubojanski - firma@gaudiumsoft.pl.
% based on OsmAnd original _config.p file.

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['TurnLeft.ogg']).
turn('left_sh', ['SharpLeft.ogg']).
turn('left_sl', ['BearLeft.ogg']).
turn('right', ['TurnRight.ogg']).
turn('right_sh', ['SharpRight.ogg']).
turn('right_sl', ['BearRight.ogg']).
turn('right_keep', ['KeepRight.ogg']).
turn('left_keep', ['KeepLeft.ogg']).


prepare_turn(Turn, Dist) == ['Straight.ogg'] :- Dist >= 1000.
prepare_turn(Turn, Dist) == ['After.ogg', delay_250, D, delay_250, M] :- Dist < 1000,
			distance(Dist) == D, turn(Turn, M).

turn_after(Turn, Dist) == ['After.ogg', delay_250, D, delay_250, M] :-
			distance(Dist) == D, turn(Turn, M).


turn(Turn, Dist) == W :- Dist < 1000,
		turn_after(Turn, Dist) == W.
		
turn(Turn, Dist) == W :- Dist >= 1000,
		W == ['Straight.ogg'].


			
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['After.ogg', delay_300, D, delay_300,'UTurn.ogg'] :- Dist < 1000,
		distance(Dist) == D.

prepare_make_ut(Dist) == ['Straight.ogg'] :- Dist >= 1000.

prepare_roundabout(Dist) == [].

make_ut(Dist) == ['After.ogg', delay_300, D, delay_300, 'UTurn.ogg'] :-
			distance(Dist) == D.
make_ut == ['UTurn.ogg'].

roundabout(Dist, _Angle, Exit) == ['After.ogg', delay_300, D, 
		delay_250, E] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == [ E] :- nth(Exit, E).

and_arrive_destination == ['Straight.ogg']. % Miss and?
then == ['Then.ogg', delay_350].
reached_destination == ['Arrive.ogg'].
bear_right == ['KeepRight.ogg'].
bear_left == ['KeepLeft.ogg'].
route_recalc(_Dist) == []. % [].  %nothing to said possibly beep?
route_new_calc(Dist) == ['Depart.ogg']. % [].  %nothing to said possibly beep?

go_ahead(Dist) == ['Straight.ogg', delay_250,  D] :-
		distance(Dist) == D.
		
go_ahead == ['Straight.ogg'].

%% 
nth(1, 'FirstExitRoundabout.ogg').
nth(2, 'SecondExitRoundabout.ogg').
nth(3, 'ThirdExitRoundabout.ogg').
nth(4, 'FourthExitRoundabout.ogg').
nth(5, 'FifthExitRoundabout.ogg').
nth(6, 'SixthExitRoundabout.ogg').
nth(N, 'GoAroundRoundabout.ogg') :- N > 6.


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'Meters.ogg',T).
dist(D, ['50.ogg']) :-  D < 60, !.
dist(D, ['80.ogg']) :-  D < 90, !.
dist(D, ['100.ogg']) :-  D < 150, !.
dist(D, ['200.ogg']) :-  D < 250, !.
dist(D, ['300.ogg']) :-  D < 350, !.
dist(D, ['400.ogg']) :-  D < 450, !.
dist(D, ['500.ogg']) :-  D < 550, !.
dist(D, ['600.ogg']) :-  D < 650, !.
dist(D, ['700.ogg']) :-  D < 750, !.
dist(D, ['800.ogg']) :-  D < 1000, !.

% distance(Dist) == [] :- Dist >= 1000.

%% resolve command main method
%% if you are familar with Prolog you can input specific to the whole mechanism,
%% by adding exception cases.
flatten(X, Y) :- flatten(X, [], Y), !.
flatten([], Acc, Acc).
flatten([X|Y], Acc, Res):- 
		flatten(Y, Acc, R), flatten(X, R, Res).
flatten(X, Acc, [X|Acc]).

resolve(X, Y) :- resolve_impl(X,Z), flatten(Z, Y).
resolve_impl([],[]).
resolve_impl([X|Rest], List) :- resolve_impl(Rest, Tail), ((X == L) -> append(L, Tail, List); List = Tail).
