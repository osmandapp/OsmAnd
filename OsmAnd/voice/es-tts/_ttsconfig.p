:- op('==', xfy, 500).
version(102).
language(es).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['gira a izquierda ']).
turn('left_sh', ['gira fuerte a la izquierda ']).
turn('left_sl', ['gira leve a la izquierda ']).
turn('right', ['gira a la derecha ']).
turn('right_sh', ['gira fuerte a la derecha ']).
turn('right_sl', ['gira leve a la derecha ']).

prepare_turn(Turn, Dist) == ['Después de', D,' prepararse para ', M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Después de ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == [ 'Después de ', D,' prepararse para dar la vuelta'] :- distance(Dist) == D.
make_ut(Dist) == [' Después de', D, ' da la vuelta'] :- distance(Dist) == D.
make_ut == ['da la vuelta'].
make_ut_wp == ['Cuando posible, da la vuelta'].

prepare_roundabout(Dist) == [ 'Después de ', D,' prepararse para entrar '] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Después de ', D, ' entra a la glorieta y toma la ', E ] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['toma la ', E ] :- nth(Exit, E).

go_ahead == ['Continue así '].
go_ahead(Dist) == ['Continue por ',  D]:- distance(Dist) == D.

and_arrive_destination == ['y llegará a su destino'].

then == ['Después '].
reached_destination == ['ha llegado a su destino'].
bear_right == ['manténga a la derecha'].
bear_left == ['manténga a la izquierda'].

route_new_calc(Dist) == ['El viaje es ', D] :- distance(Dist) == D.  
route_recalc(Dist) == ['Ruta recalculada, el viaje es ', D] :- distance(Dist) == D.

location_lost == ['g p s señal perdido '].


%% 
nth(1, 'primera salida').
nth(2, 'segunda salida').
nth(3, 'tercera salida').
nth(4, 'cuarta salida').
nth(5, 'quinta salida').
nth(6, 'sexta salida').
nth(7, 'séptima salida').
nth(8, 'octava salida').
nth(9, 'novena salida').
nth(10, 'décima salida').
nth(11, 'salida once').
nth(12, 'salida doce').
nth(13, 'salida trece').
nth(14, 'salida catorce').
nth(15, 'salida quince').
nth(16, 'salida diezseis').
nth(17, 'salida diezsiete').


%%% distance measure
distance(Dist) == [ X, ' metros'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist) == [ X, ' metros'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist) == ['cerca de un kilómetro '] :- Dist < 1500.
distance(Dist) == ['cerca de ', X, ' kilómetros '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, ' kilómetros '] :- D is round(Dist/1000), num_atom(D, X).


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