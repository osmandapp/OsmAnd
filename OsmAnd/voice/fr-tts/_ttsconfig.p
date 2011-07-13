:- op('==', xfy, 500).
version(100).
language(fr).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['tournez à gauche ']).
turn('left_sh', ['immédiatement à gauche ']).
turn('left_sl', ['tournez légèrement à gauche ']).
turn('right', ['tournez à droite ']).
turn('right_sh', ['immédiatement à droite ']).
turn('right_sl', ['tournez légèrement à droite ']).

prepare_turn(Turn, Dist) == ['Préparez vous à ', M, ' dans ', D] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Dans ', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['Dans ', D, ' préparez vous à faire demi tour'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['Préparez vous à entrer dans le rondpoint dans ', D] :- 
		distance(Dist) == D.

make_ut(Dist) == ['Dans ', D, ' faites demi-tour '] :- 
			distance(Dist) == D.
make_ut == ['Faites demi-tour '].

roundabout(Dist, _Angle, Exit) == ['Dans ', D, ' entrez dans le rond-point et prenez la ', E, 'sortie'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['prenez la ', E, 'sortie'] :- nth(Exit, E).

and_arrive_destination == ['et arrivez à destination ']. % Miss and?
then == ['puis '].
reached_destination == ['vous êtes arrivé à destination '].
bear_right == ['serrez à droite '].
bear_left == ['serrez à gauche '].
route_recalc(_Dist) == []. % ['recalcul de l itinéraire '].  %nothing to said possibly beep?	
route_new_calc(Dist) == ['L itinéraire fait  ', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['Continuez pendant ', D]:- distance(Dist) == D.
go_ahead == ['Continuez tout droit '].

%% 
nth(1, '1ère ').
nth(2, '2ème ').
nth(3, '3ème ').
nth(4, '4ème ').
nth(5, '5ème ').
nth(6, '6ème ').
nth(7, '7ème ').
nth(8, '8ème ').
nth(9, '9ème ').
nth(10, '10ème ').
nth(11, '11ème ').
nth(12, '12ème ').
nth(13, '13ème ').
nth(14, '14ème ').
nth(15, '15ème ').
nth(16, '16ème ').
nth(17, '17ème ').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, ' metres',T).
dist(D, ['10 ']) :-  D < 15, !.
dist(D, ['20 ']) :-  D < 25, !.
dist(D, ['30 ']) :-  D < 35, !.
dist(D, ['40 ']) :-  D < 45, !.
dist(D, ['50 ']) :-  D < 55, !.
dist(D, ['60 ']) :-  D < 65, !.
dist(D, ['70 ']) :-  D < 75, !.
dist(D, ['80 ']) :-  D < 85, !.
dist(D, ['90 ']) :-  D < 95, !.
dist(D, ['100 ']) :-  D < 125, !.
dist(D, ['150 ']) :-  D < 175, !.
dist(D, ['200 ']) :-  D < 225, !.
dist(D, ['250 ']) :-  D < 275, !.
dist(D, ['300 ']) :-  D < 325, !.
dist(D, ['350 ']) :-  D < 375, !.
dist(D, ['400 ']) :-  D < 425, !.
dist(D, ['450 ']) :-  D < 475, !.
dist(D, ['500 ']) :-  D < 525, !.
dist(D, ['550 ']) :-  D < 575, !.
dist(D, ['600 ']) :-  D < 625, !.
dist(D, ['650 ']) :-  D < 675, !.
dist(D, ['700 ']) :-  D < 725, !.
dist(D, ['750 ']) :-  D < 775, !.
dist(D, ['800 ']) :-  D < 825, !.
dist(D, ['850 ']) :-  D < 875, !.
dist(D, ['900 ']) :-  D < 925, !.
dist(D, ['950 ']) :-  D < 975, !.
dist(D, ['1000 ']) :-  !.

distance(Dist) == ['quelque 1 kilometre '] :- Dist < 1500.
distance(Dist) == ['quelques 2 kilometres '] :- Dist < 2500.
distance(Dist) == ['quelques 3 kilometres '] :- Dist < 3500.
distance(Dist) == ['quelques 4 kilometres '] :- Dist < 4500.
distance(Dist) == ['quelques 5 kilometres '] :- Dist < 5500.
distance(Dist) == ['quelques 6 kilometres '] :- Dist < 6500.
distance(Dist) == ['quelques 7 kilometres '] :- Dist < 7700.
distance(Dist) == ['quelques 8 kilometres '] :- Dist < 8500.
distance(Dist) == ['quelques 9 kilometres '] :- Dist < 9500.
distance(Dist) == ['quelques ', X, ' kilomètres '] :- D is Dist/1000, dist(D, X).

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
