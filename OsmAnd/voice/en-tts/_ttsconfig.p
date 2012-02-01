:- op('==', xfy, 500).
version(101).
language(en).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['turn left ']).
turn('left_sh', ['turn sharp left ']).
turn('left_sl', ['turn slightly left ']).
turn('right', ['turn right ']).
turn('right_sh', ['turn sharp right ']).
turn('right_sl', ['turn slightly right ']).

prepare_turn(Turn, Dist) == ['Prepare to ', M, ' after ', D] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['After ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Prepare to turn back after ', D] :- distance(Dist) == D.
make_ut(Dist) == ['After ', D, ' turn back '] :- distance(Dist) == D.
make_ut == ['Please make a U turn '].

prepare_roundabout(Dist) == ['Prepare to enter a roundabout after ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['After ', D, ' enter the roundabout, and take the ', E, 'exit'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['take the ', E, 'exit'] :- nth(Exit, E).

go_ahead == ['Continue straight ahead '].
go_ahead(Dist) == ['Follow the course of the road for ', D]:- distance(Dist) == D.

and_arrive_destination == ['and arrive at your destination '].

then == ['then '].
reached_destination == ['you have reached your destination '].
bear_right == ['keep right '].
bear_left == ['keep left '].

route_new_calc(Dist) == ['The trip is ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Route recalculated, distance ', D] :- distance(Dist) == D.

location_lost == ['g p s location lost '].


%% 
nth(1, 'first ').
nth(2, 'second ').
nth(3, 'third ').
nth(4, 'fourth ').
nth(5, 'fifth ').
nth(6, 'sixth ').
nth(7, 'seventh ').
nth(8, 'eight ').
nth(9, 'nineth ').
nth(10, 'tenth ').
nth(11, 'eleventh ').
nth(12, 'twelfth ').
nth(13, 'thirteenth ').
nth(14, 'fourteenth ').
nth(15, 'fifteenth ').
nth(16, 'sixteenth ').
nth(17, 'seventeenth ').


%%% distance measure
distance(Dist) == [ X, ' meters'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist) == [ X, ' meters'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist) == ['about 1 kilometer '] :- Dist < 1500.
distance(Dist) == ['about ', X, ' kilometers '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, ' kilometers '] :- D is round(Dist/1000), num_atom(D, X).


%% resolve command main method
%% if you are familar with Prolog you can input specific to the whole mechanism,
%% by adding exception cases.
flatten(X, Y) :- flatten(X, [], Y), !.
flatten([], Acc, Acc).
flatten([X|Y], Acc, Res):- flatten(Y, Acc, R), flatten(X, R, Res).
flatten(X, Acc, [X|Acc]).

resolve(X, Y) :- resolve_impl(X,Z), flatten(Z, Y).
resolve_impl([],[]).
resolve_impl([X|Rest], List) :- resolve_impl(Rest, Tail), ((X == L) -> append(L, Tail, List); List = Tail).