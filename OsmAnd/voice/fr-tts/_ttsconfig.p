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
dist(D, ['10 ']) :-  D < 20, !.
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

distance(Dist) == ['plus de 1 kilometre '] :- Dist < 1500.
distance(Dist) == ['plus de 2 kilometres '] :- Dist < 3000.
distance(Dist) == ['plus de 3 kilometres '] :- Dist < 4000.
distance(Dist) == ['plus de 4 kilometres '] :- Dist < 5000.
distance(Dist) == ['plus de 5 kilometres '] :- Dist < 6000.
distance(Dist) == ['plus de 6 kilometres '] :- Dist < 7000.
distance(Dist) == ['plus de 7 kilometres '] :- Dist < 8000.
distance(Dist) == ['plus de 8 kilometres '] :- Dist < 9000.
distance(Dist) == ['plus de 9 kilometres '] :- Dist < 10000.
distance(Dist) == ['plus de ', X, ' kilomètres '] :- D is Dist/1000, dist(D, X).

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
