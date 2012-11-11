:- op('==', xfy, 500).
version(101).
language(pt).

% before each announcement (beep)
preamble - [].


%% TURNS
turn_imp('left', ['virar à esquerda ']).
turn_imp('left_sh', ['virar acentuadamente à esquerda ']).
turn_imp('left_sl', ['virar ligeiramente à esquerda ']).
turn_imp('right', ['virar à direita ']).
turn_imp('right_sh', ['virar acentuadamente à direita ']).
turn_imp('right_sl', ['virar ligeiramente à direita ']).
turn_imp('right_keep', ['manter-se à direita']).
turn_imp('left_keep', ['manter-se à esquerda']).

%% Second form
turn('left', ['vire à esquerda ']).
turn('left_sh', ['vire acentuadamente à esquerda ']).
turn('left_sl', ['vire ligeiramente à esquerda ']).
turn('right', ['vire à direita ']).
turn('right_sh', ['vire acentuadamente à direita ']).
turn('right_sl', ['vire ligeiramente à direita ']).
turn('right_keep', ['mantenha-se à direita']).
turn('left_keep', ['mantenha-se à esquerda']).

prepare_turn(Turn, Dist) == ['Prepare-se para ', M, ' após ', D] :- distance(Dist) == D, turn_imp(Turn, M).
turn(Turn, Dist) == ['Após ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Prepare-se para retornar após ', D] :- distance(Dist) == D.
make_ut(Dist) == ['Após ', D, ' faça um retorno '] :- distance(Dist) == D.
make_ut == ['Faça um retorno '].
make_ut_wp == ['Retorne assim que possível '].

prepare_roundabout(Dist) == ['Prepare-se para entrar na rotatória após ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Após ', D, ' entre na rotatória e pegue a ', E, 'saída'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['pegue a ', E, 'saída'] :- nth(Exit, E).

go_ahead == ['Siga em frente '].
go_ahead(Dist) == ['Siga o caminho por ', D]:- distance(Dist) == D.

and_arrive_destination == ['e chegou ao seu destino '].

then == ['então '].
reached_destination == ['você chegou ao seu destino '].
and_arrive_intermediate == ['e chegou ao seu ponto de passagem '].
reached_intermediate == ['você chegou ao seu ponto de passagem'].
bear_right == ['mantenha-se à direita '].
bear_left == ['mantenha-se à esquerda '].

route_new_calc(Dist) == ['A rota possui ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Rota recalculada. Distância é de ', D] :- distance(Dist) == D.

location_lost == ['sem sinal g p s '].


%%
nth(1, 'primeira ').
nth(2, 'segunda ').
nth(3, 'terceira ').
nth(4, 'quarta ').
nth(5, 'quinta ').
nth(6, 'sexta ').
nth(7, 'sétima ').
nth(8, 'oitava ').
nth(9, 'nona ').
nth(10, 'décima ').
nth(11, 'décima primeira ').
nth(12, 'décima segunda ').
nth(13, 'décima terceira ').
nth(14, 'décima quarta ').
nth(15, 'décima quinta ').
nth(16, 'décima sexta ').
nth(17, 'décima sétima ').


distance(Dist) == D :- measure('km-m'), distance_km(Dist) == D.
distance(Dist) == D :- measure('mi-f'), distance_mi_f(Dist) == D.
distance(Dist) == D :- measure('mi-y'), distance_mi_y(Dist) == D.

%%% distance measure km/m
distance_km(Dist) == [ X, ' metros']               :- Dist < 100,   D is round(Dist/10.0)*10,           num_atom(D, X).
distance_km(Dist) == [ X, ' metros']               :- Dist < 1000,  D is round(2*Dist/100.0)*50,        num_atom(D, X).
distance_km(Dist) == ['aproximadamente um quilômetro ']        :- Dist < 1500.
distance_km(Dist) == ['aproximadamente ', X, ' quilômetros '] :- Dist < 10000, D is round(Dist/1000.0),            num_atom(D, X).
distance_km(Dist) == [ X, ' quilômetros ']          :-               D is round(Dist/1000.0),            num_atom(D, X).

%%% distance measure mi/f
distance_mi_f(Dist) == [ X, ' pés']               :- Dist < 160,   D is round(2*Dist/100.0/0.3048)*50, num_atom(D, X).
distance_mi_f(Dist) == [ X, ' um décimo de milha']    :- Dist < 241,   D is round(Dist/161.0),             num_atom(D, X).
distance_mi_f(Dist) == [ X, ' décimos de milha']   :- Dist < 1529,  D is round(Dist/161.0),             num_atom(D, X).
distance_mi_f(Dist) == ['aproximadamente uma milha ']           :- Dist < 2414.
distance_mi_f(Dist) == ['aproximadamente ', X, ' milhas ']    :- Dist < 16093, D is round(Dist/1609.0),            num_atom(D, X).
distance_mi_f(Dist) == [ X, ' milhas ']             :-               D is round(Dist/1609.0),            num_atom(D, X).

%%% distance measure mi/y
distance_mi_y(Dist) == [ X, ' jardas']              :- Dist < 241,   D is round(Dist/10.0/0.9144)*10,    num_atom(D, X).
distance_mi_y(Dist) == [ X, ' jardas']              :- Dist < 1300,  D is round(2*Dist/100.0/0.9144)*50, num_atom(D, X).
distance_mi_y(Dist) == ['aproximadamente uma milha ']           :- Dist < 2414.
distance_mi_y(Dist) == ['aproximadamente ', X, ' milhas ']    :- Dist < 16093, D is round(Dist/1609.0),            num_atom(D, X).
distance_mi_y(Dist) == [ X, ' milhas ']             :-               D is round(Dist/1609.0),            num_atom(D, X).


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
