:- op('==', xfy, 500).
version(100).
language(en).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['turn left ']).
turn('left_sh', ['sharp left ']).
turn('left_sl', ['turn slightly left ']).
turn('right', ['turn right ']).
turn('right_sh', ['sharp right ']).
turn('right_sl', ['turn slightly right ']).

prepare_turn(Turn, Dist) == ['Prepare to ', M, ' after ', D] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['After ', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['Prepare to turn back after ', D] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['Prepare to enter a roundabout after ', D] :- 
		distance(Dist) == D.

make_ut(Dist) == ['After ', D, ' turn back '] :- 
			distance(Dist) == D.
make_ut == ['Make U turn '].

roundabout(Dist, _Angle, Exit) == ['After ', D, ' enter the roundabout, and take the ', E, 'exit'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['take the ', E, 'exit'] :- nth(Exit, E).

and_arrive_destination == ['and arrive at your destination ']. % Miss and?
then == ['then '].
reached_destination == ['you have reached your destination '].
bear_right == ['keep right '].
bear_left == ['keep left '].
route_recalc(_Dist) == []. % ['recalculating route '].  %nothing to said possibly beep?	
route_new_calc(Dist) == ['The trip is ', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['Drive for ', D]:- distance(Dist) == D.
go_ahead == ['Continue straight ahead '].

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
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, ' meters',T).
dist(D, ['10 ']) :-  D < 15, !.
dist(D, ['15 ']) :-  D < 20, !.
dist(D, ['20 ']) :-  D < 30, !.
dist(D, ['30 ']) :-  D < 40, !.
dist(D, ['40 ']) :-  D < 50, !.
dist(D, ['50 ']) :-  D < 60, !.
dist(D, ['60 ']) :-  D < 70, !.
dist(D, ['70 ']) :-  D < 80, !.
dist(D, ['80 ']) :-  D < 90, !.
dist(D, ['90 ']) :-  D < 100, !.
dist(D, ['100 ']) :-  D < 150, !.
dist(D, ['150 ']) :-  D < 200, !.
dist(D, ['200 ']) :-  D < 250, !.
dist(D, ['250 ']) :-  D < 300, !.
dist(D, ['300 ']) :-  D < 350, !.
dist(D, ['350 ']) :-  D < 400, !.
dist(D, ['400 ']) :-  D < 450, !.
dist(D, ['450 ']) :-  D < 500, !.
dist(D, ['500 ']) :-  D < 550, !.
dist(D, ['550 ']) :-  D < 600, !.
dist(D, ['600 ']) :-  D < 650, !.
dist(D, ['650 ']) :-  D < 700, !.
dist(D, ['700 ']) :-  D < 750, !.
dist(D, ['750 ']) :-  D < 800, !.
dist(D, ['800 ']) :-  D < 850, !.
dist(D, ['850 ']) :-  D < 900, !.
dist(D, ['900 ']) :-  D < 950, !.
dist(D, ['950 ']) :-  !.

distance(Dist) == ['about 1 kilometer '] :- Dist < 1500.
distance(Dist) == ['about 2 kilometers '] :- Dist < 2500.
distance(Dist) == ['about 3 kilometers '] :- Dist < 3500.
distance(Dist) == ['about 4 kilometers '] :- Dist < 4500.
distance(Dist) == ['about 5 kilometers '] :- Dist < 5500.
distance(Dist) == ['about 6 kilometers '] :- Dist < 6500.
distance(Dist) == ['about 7 kilometers '] :- Dist < 7500.
distance(Dist) == ['about 8 kilometers '] :- Dist < 8500.
distance(Dist) == ['about 9 kilometers '] :- Dist < 9500.
distance(Dist) == ['just over ', X, ' kilometers '] :- D is Dist/1000, dist(D, X).

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