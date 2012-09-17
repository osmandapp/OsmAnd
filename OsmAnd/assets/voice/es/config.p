:- op('==', xfy, 500).
version(0).


% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['giro_a.ogg',   'izquierda.ogg']).
turn('left_sh', ['giro_fuerte.ogg', 'izquierda']).
turn('left_sl', ['giro_leve.ogg', 'izquierda.ogg']).
turn('right', ['giro_a.ogg', 'derecha.ogg']).
turn('right_sh', ['giro_fuerte.ogg', 'derecha']).
turn('right_sl', ['giro_leve.ogg', 'derecha.ogg']).
turn('right_keep', ['mantener_derecha.ogg']).
turn('left_keep', ['mantener_izquierda.ogg']).

prepare_turn(Turn, Dist) == ['despues_de.ogg', delay_450, D,'prepararse_para.ogg', delay_450, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['despues_de.ogg', delay_250, D, delay_250, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == [ 'despues_de.ogg', delay_300, D,'prepararse_para.ogg', delay_300,'giro_en_u.ogg'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == [ 'despues_de.ogg', delay_300, D,'prepara_entrar.ogg'] :- 
		distance(Dist) == D.

make_ut(Dist) == ['despues_de.ogg', delay_300, D, delay_300, 'giro_en_u.ogg'] :- 
			distance(Dist) == D.
make_ut == ['giro_en_u.ogg'].

roundabout(Dist, _Angle, Exit) == ['despues_de.ogg', delay_300, D, delay_300, 'entrar_glorieta.ogg', delay_250, 'y_tomar.ogg', 
		delay_250, E, 'salida.ogg'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['tomando la.ogg', delay_250,  E, 'salida.ogg'] :- nth(Exit, E).

and_arrive_destination == ['destino.ogg']. % Miss and?
and_arrive_intermediate == ['and arrive at your via point '].
reached_intermediate == ['you have reached your via point'].
then == ['entonces.ogg', delay_350].
reached_destination == ['llegado.ogg'].
bear_right == ['mantener_derecha.ogg'].
bear_left == ['mantener_izquierda.ogg'].
route_recalc(_Dist) == []. % nothing to said possibly beep?
route_new_calc(_Dist) == []. % nothing to said possibly beep?	

go_ahead(Dist) == ['siga_por.ogg', delay_250,  D]:- distance(Dist) == D.
go_ahead == ['siga.ogg'].

%% 
nth(1, 'primera.ogg').
nth(2, 'segunda.ogg').
nth(3, 'tercera.ogg').
nth(4, 'cuarta.ogg').
nth(5, 'quinta.ogg').
nth(6, 'sexta.ogg').
nth(7, 'septima.ogg').
nth(8, 'octava.ogg').
nth(9, 'novena.ogg').
nth(10, 'decima.ogg').
nth(11, 'once.ogg').
nth(12, 'doce.ogg').
nth(13, 'trece.ogg').
nth(14, 'catorce.ogg').
nth(15, 'quince.ogg').
nth(16, 'diezseis.ogg').
nth(17, 'diezsiete.ogg').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'metros.ogg',T).
dist(D, ['diez.ogg']) :-  D < 20, !.
dist(D, ['veinte.ogg']) :-  D < 30, !.
dist(D, ['treinta.ogg']) :-  D < 40, !.
dist(D, ['cuarenta.ogg']) :-  D < 50, !.
dist(D, ['cincuenta.ogg']) :-  D < 60, !.
dist(D, ['sesenta.ogg']) :-  D < 70, !.
dist(D, ['setenta.ogg']) :-  D < 80, !.
dist(D, ['ochenta.ogg']) :-  D < 90, !.
dist(D, ['noventa.ogg']) :-  D < 100, !.
dist(D, ['cien.ogg']) :-  D < 150, !.
dist(D, ['ciento.ogg', 'cincuenta.ogg']) :-  D < 200, !.
dist(D, ['dos.ogg','cientos.ogg']) :-  D < 250, !.
dist(D, ['dos.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 300, !.
dist(D, ['tres.ogg','cientos.ogg']) :-  D < 350, !.
dist(D, ['tres.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 400, !.
dist(D, ['cuatro.ogg','cientos.ogg']) :-  D < 450, !.
dist(D, ['cuatro.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 500, !.
dist(D, ['quinientos.ogg']) :-  D < 550, !.
dist(D, ['quinientos.ogg', 'cincuenta.ogg']) :-  D < 600, !.
dist(D, ['seis.ogg','cientos.ogg']) :-  D < 650, !.
dist(D, ['seis.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 700, !.
dist(D, ['setescientos.ogg']) :-  D < 750, !.
dist(D, ['setescientos.ogg', 'cincuenta.ogg']) :-  D < 800, !.
dist(D, ['ocho.ogg','cientos.ogg']) :-  D < 850, !.
dist(D, ['ocho.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 900, !.
dist(D, ['novecientos.ogg']) :-  D < 950, !.
dist(D, ['novecientos.ogg', 'cincuenta.ogg']) :-  !.


distance(Dist) == ['mas_de.ogg', 'un.ogg', 'kilometro.ogg'] :- Dist < 1500.
distance(Dist) == ['mas_de.ogg', 'dos.ogg', 'kilometros.ogg'] :- Dist < 3000.
distance(Dist) == ['mas_de.ogg', 'tres.ogg', 'kilometros.ogg'] :- Dist < 4000.
distance(Dist) == ['mas_de.ogg', 'cuatro.ogg', 'kilometros.ogg'] :- Dist < 5000.
distance(Dist) == ['mas_de.ogg', 'cinco.ogg', 'kilometros.ogg'] :- Dist < 6000.
distance(Dist) == ['mas_de.ogg', 'seis.ogg', 'kilometros.ogg'] :- Dist < 7000.
distance(Dist) == ['mas_de.ogg', 'siete.ogg', 'kilometros.ogg'] :- Dist < 8000.
distance(Dist) == ['mas_de.ogg', 'ocho.ogg', 'kilometros.ogg'] :- Dist < 9000.
distance(Dist) == ['mas_de.ogg', 'nueve.ogg', 'kilometros.ogg'] :- Dist < 10000.
distance(Dist) == ['mas_de.ogg', X, 'kilometros.ogg'] :- D is Dist/1000, dist(D, X).



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