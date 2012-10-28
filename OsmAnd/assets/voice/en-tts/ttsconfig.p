:- op('==', xfy, 500).
version(101).
language(en).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['turn left ']).
turn('left_sh', ['turn sharply left ']).
turn('left_sl', ['turn slightly left ']).
turn('right', ['turn right ']).
turn('right_sh', ['turn sharply right ']).
turn('right_sl', ['turn slightly right ']).
turn('right_keep', ['keep right']).
turn('left_keep', ['keep left']).

prepare_turn(Turn, Dist) == ['Prepare to ', M, ' after ', D] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['After ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Prepare to make a U turn after ', D] :- distance(Dist) == D.
make_ut(Dist) == ['After ', D, ' make a U turn '] :- distance(Dist) == D.
make_ut == ['Make a U turn '].
make_ut_wp == ['When possible, please make a U turn '].

prepare_roundabout(Dist) == ['Prepare to enter a roundabout after ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['After ', D, ' enter the roundabout, and take the ', E, 'exit'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['take the ', E, 'exit'] :- nth(Exit, E).

go_ahead == ['Go straight ahead '].
go_ahead(Dist) == ['Follow the course of the road for ', D]:- distance(Dist) == D.

and_arrive_destination == ['and arrive at your destination '].

then == ['then '].
reached_destination == ['you have reached your destination '].
and_arrive_intermediate == ['and arrive at your waypoint '].
reached_intermediate == ['you have reached your waypoint'].
bear_right == ['keep right '].
bear_left == ['keep left '].

route_new_calc(Dist) == ['The trip is ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Route recalculated, distance ', D] :- distance(Dist) == D.

location_lost == ['g p s signal lost '].


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


distance(Dist) == D :- measure('km-m'), distance_km(Dist) == D.
distance(Dist) == D :- measure('mi-f'), distance_mi_f(Dist) == D.
distance(Dist) == D :- measure('mi-y'), distance_mi_y(Dist) == D.

%%% distance measure km/m
distance_km(Dist) == [ X, ' meters']               :- Dist < 100,   D is round(Dist/10.0)*10,           num_atom(D, X).
distance_km(Dist) == [ X, ' meters']               :- Dist < 1000,  D is round(2*Dist/100.0)*50,        num_atom(D, X).
distance_km(Dist) == ['about 1 kilometer ']        :- Dist < 1500.
distance_km(Dist) == ['about ', X, ' kilometers '] :- Dist < 10000, D is round(Dist/1000.0),            num_atom(D, X).
distance_km(Dist) == [ X, ' kilometers ']          :-               D is round(Dist/1000.0),            num_atom(D, X).

%%% distance measure mi/f
distance_mi_f(Dist) == [ X, ' feet']               :- Dist < 160,   D is round(2*Dist/100.0/0.3048)*50, num_atom(D, X).
distance_mi_f(Dist) == [ X, ' tenth of a mile']    :- Dist < 241,   D is round(Dist/161.0),             num_atom(D, X).
distance_mi_f(Dist) == [ X, ' tenths of a mile']   :- Dist < 1529,  D is round(Dist/161.0),             num_atom(D, X).
distance_mi_f(Dist) == ['about 1 mile ']           :- Dist < 2414.
distance_mi_f(Dist) == ['about ', X, ' miles ']    :- Dist < 16093, D is round(Dist/1609.0),            num_atom(D, X).
distance_mi_f(Dist) == [ X, ' miles ']             :-               D is round(Dist/1609.0),            num_atom(D, X).

%%% distance measure mi/y
distance_mi_y(Dist) == [ X, ' yards']              :- Dist < 241,   D is round(Dist/10.0/0.9144)*10,    num_atom(D, X).
distance_mi_y(Dist) == [ X, ' yards']              :- Dist < 1300,  D is round(2*Dist/100.0/0.9144)*50, num_atom(D, X).
distance_mi_y(Dist) == ['about 1 mile ']           :- Dist < 2414.
distance_mi_y(Dist) == ['about ', X, ' miles ']    :- Dist < 16093, D is round(Dist/1609.0),            num_atom(D, X).
distance_mi_y(Dist) == [ X, ' miles ']             :-               D is round(Dist/1609.0),            num_atom(D, X).


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