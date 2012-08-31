:- op('==', xfy, 500).
version(101).
language(es).

% before each announcement (beep)
preamble - [].


%% TURNS

turn('left', ['gira a la izquierda ']).
turn('left_sh', ['gira fuerte a la izquierda ']).
turn('left_sl', ['gira levemente a la izquierda ']).
turn('right', ['gira a la derecha ']).
turn('right_sh', ['gira fuerte a la derecha ']).
turn('right_sl', ['gira levemente a la derecha ']).
turn('right_keep', ['mantente a la derecha']).
turn('left_keep', ['mantente a la izquierda']).

turn_inf('left', ['girar a la izquierda ']).
turn_inf('left_sh', ['girar fuerte a la izquierda ']).
turn_inf('left_sl', ['girar levemente a la izquierda ']).
turn_inf('right', ['girar a la derecha ']).
turn_inf('right_sh', ['girar fuerte a la derecha ']).
turn_inf('right_sl', ['girar levemente a la derecha ']).
turn_inf('right_keep', ['mantente a la derecha']).
turn_inf('left_keep', ['mantente a la izquierda']).

prepare_turn(Turn, Dist) == ['Prepárate para ', M, ' tras ', D] :- distance(Dist) == D, turn_inf(Turn, M).
turn(Turn, Dist) == ['Tras ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == [ 'Prepárate para dar la vuelta tras ', D] :- distance(Dist) == D.
make_ut(Dist) == ['Tras ', D, ' da la vuelta'] :- distance(Dist) == D.
make_ut == ['Da la vuelta'].
make_ut_wp == ['Cuando sea posible, da la vuelta'].

prepare_roundabout(Dist) == [ 'Prepárate para entrar en la rotonda tras ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Tras ', D, ' entra en la rotonda y toma la ', E, ' salida' ] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['toma la ', E, ' salida' ] :- nth(Exit, E).


go_ahead == ['Continúa recto'].
go_ahead(Dist) == ['Sigue la vía durante ', D]:- distance(Dist) == D.


and_arrive_destination == ['y llegarás a tu destino'].

then == ['. Luego '].

reached_destination == ['has llegado a tu destino'].

bear_right == ['mantente a la derecha'].
bear_left == ['mantente a la izquierda'].

route_new_calc(Dist) == ['El camino es ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Ruta recalculada, distancia ', D] :- distance(Dist) == D.

location_lost == ['señal g p s perdida '].


%%

nth(1, 'primera').
nth(2, 'segunda').
nth(3, 'tercera').
nth(4, 'cuarta').
nth(5, 'quinta').
nth(6, 'sexta').
nth(7, 'séptima').
nth(8, 'octava').
nth(9, 'novena').
nth(10, 'décima').
nth(11, 'undécima').
nth(12, 'duodécima').
nth(13, 'decimotercera').
nth(14, 'decimocuarta').
nth(15, 'decimoquinta').
nth(16, 'decimosexta').
nth(17, 'decimoséptima').


%%% distance measure

distance(Dist) == [ X, ' metros'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' metros'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['cerca de un kilómetro '] :- Dist < 1500.
distance(Dist) == ['cerca de ', X, ' kilómetros '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' kilómetros '] :- D is round(Dist/1000.0), num_atom(D, X).


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
