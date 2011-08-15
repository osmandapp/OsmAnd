:- op('==', xfy, 500).
version(101).
language(es).

% before each announcement (beep)
preamble - [].

%% TURNS 
turn('left', ['giro a izquierda ']).
turn('left_sh', ['giro fuerte a izquierda ']).
turn('left_sl', ['giro leve a izquierda ']).
turn('right', ['giro a derecha ']).
turn('right_sh', ['giro fuerte a derecha ']).
turn('right_sl', ['giro leve a derecha ']).

prepare_turn(Turn, Dist) == ['Despues de', D,' prepararse para ', M] :- 
   distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Despues de ', D, M] :- 
   distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == [ 'Despues de ', D,' prepararse para giro en u'] :- 
   distance(Dist) == D.

prepare_roundabout(Dist) == [ 'Despues de ', D,' prepararse para entrar '] :- 
   distance(Dist) == D.

make_ut(Dist) == [' Despues de', D, ' giro en u'] :- 
   distance(Dist) == D.
make_ut == ['Giro en u'].

roundabout(Dist, _Angle, Exit) == ['Despues de ', D, ' entrar a glorieta y tomar la ', 
   E ] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['tomando la ', E ] :- nth(Exit, E).

and_arrive_destination == ['y llegará a'].
then == ['Despues '].
reached_destination == ['ha llegado a su destino'].
bear_right == ['manténga a su derecha'].
bear_left == ['manténga a su izquierda'].
route_recalc(_Dist) == []. % ['recalculating route ']. nothing to said possibly beep?
route_new_calc(_Dist) == ['El viaje es ', D] :- distance(Dist) == D. % nothing to said possibly beep?	

go_ahead(Dist) == ['Continue por ',  D]:- distance(Dist) == D.
go_ahead == ['Continue así '].

%% 
nth(1, 'primera salida').
nth(2, 'segunda salida').
nth(3, 'tercera salida').
nth(4, 'cuarta salida').
nth(5, 'quinta salida').
nth(6, 'sexta salida').
nth(7, 'salida de séptima').
nth(8, 'octava salida').
nth(9, 'salida del noveno').
nth(10, 'salida décimo').
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
distance(Dist) == ['cerca de un kilometro '] :- Dist < 1500.
distance(Dist) == ['cerca de ', X, ' kilometros '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, ' kilometros '] :- D is round(Dist/1000), num_atom(D, X).

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