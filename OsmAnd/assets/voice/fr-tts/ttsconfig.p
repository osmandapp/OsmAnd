:- op('==', xfy, 500).
version(101).
language(fr).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['tournez à gauche ']).
turn('left_sh', ['tournez immédiatement à gauche ']).
turn('left_sl', ['tournez légèrement à gauche ']).
turn('right', ['tournez à droite ']).
turn('right_sh', ['tournez immédiatement à droite ']).
turn('right_sl', ['tournez légèrement à droite ']).
turn('right_keep', ['serrez à droite ']).
turn('left_keep', ['serrez à gauche ']).

prep2turn('left', ['tourner à gauche ']).
prep2turn('left_sh', ['tourner immédiatement à gauche ']).
prep2turn('left_sl', ['tourner légèrement à gauche ']).
prep2turn('right', ['tourner à droite ']).
prep2turn('right_sh', ['tourner immédiatement à droite ']).
prep2turn('right_sl', ['tourner légèrement à droite ']).
prep2turn('right_keep', ['serrer à droite ']).
prep2turn('left_keep', ['serrer à gauche ']).


prepare_turn(Turn, Dist) == ['Dans ', D, ' préparez vous à ', M] :- distance(Dist) == D, prep2turn(Turn, M).
turn(Turn, Dist) == ['Dans ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Dans ', D, ' préparez vous à faire demi tour'] :- distance(Dist) == D.
make_ut(Dist) == ['Dans ', D, ' faites demi-tour '] :- distance(Dist) == D.
make_ut == ['Faites demi-tour '].
make_ut_wp == ['Faites demi-tour '].

prepare_roundabout(Dist) == ['Préparez vous à entrer dans le rond-point dans ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Dans ', D, ' entrez dans le rond-point et prenez la ', E, 'sortie'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['prenez la ', E, 'sortie'] :- nth(Exit, E).

go_ahead == ['Continuez tout droit '].
go_ahead(Dist) == ['Continuez pendant ', D]:- distance(Dist) == D.

and_arrive_destination == ['et arrivez à destination '].

then == ['puis '].
reached_destination == ['vous êtes arrivé à destination '].
and_arrive_intermediate == ['and arrive at your via point '].
reached_intermediate == ['you have reached your via point'].
bear_right == ['serrez à droite '].
bear_left == ['serrez à gauche '].

route_new_calc(Dist) == ['L itinéraire fait  ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['recalcul de l itinéraire, l itinéraire fait ', D] :- distance(Dist) == D.

location_lost == ['signal g p s perdu '].


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
distance(Dist) == [ X, ' mètres'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' mètres'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['environ 1 kilomètre '] :- Dist < 1500.
distance(Dist) == ['environ ', X, ' kilomètres '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' kilomètres '] :- D is round(Dist/1000.0), num_atom(D, X).


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
