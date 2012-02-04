:- op('==', xfy, 500).
version(0).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['giro_a.ogg', 'izquierda.ogg']).
turn('left_sh', ['giro_fuerte.ogg', 'izquierda.ogg']).
turn('left_sl', ['giro_leve.ogg', 'izquierda.ogg']).
turn('right', ['giro_a.ogg', 'derecha.ogg']).
turn('right_sh', ['giro_fuerte.ogg', 'derecha.ogg']).
turn('right_sl', ['giro_leve.ogg', 'derecha.ogg']).

prepare_turn(Turn, Dist) == ['despues_de.ogg', delay_450, D,'prepararse_para.ogg', delay_450, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['despues_de.ogg', delay_250, D, delay_250, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == [ 'despues_de.ogg', delay_300, D,'prepararse_para.ogg', delay_300,'giro_en_u.ogg'] :- distance(Dist) == D.
make_ut(Dist) == ['despues_de.ogg', delay_300, D, delay_300, 'giro_en_u.ogg'] :- distance(Dist) == D.
make_ut == ['giro_en_u.ogg'].

prepare_roundabout(Dist) == [ 'despues_de.ogg', delay_300, D,'prepara_entrar.ogg'] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['despues_de.ogg', delay_300, D, delay_300, 'entrar_glorieta.ogg', delay_250, 'y_tomar.ogg', 
	delay_250, E, 'salida.ogg'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['tomando la.ogg', delay_250,  E, 'salida.ogg'] :- nth(Exit, E).

go_ahead == ['siga.ogg'].
go_ahead(Dist) == ['siga_por.ogg', delay_250,  D]:- distance(Dist) == D.

and_arrive_destination == ['destino.ogg'].

then == ['entonces.ogg', delay_350].
reached_destination == ['llegado.ogg'].
bear_right == ['mantener_derecha.ogg'].
bear_left == ['mantener_izquierda.ogg'].

route_new_calc(Dist) == ['eltrayecto.ogg', D] :- distance(Dist) == D.
route_recalc(Dist) == ['recalcular.ogg', D] :- distance(Dist) == D.

location_lost == ['gps_signal_lost.ogg'].


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
dist(D, ['diez.ogg']) :-  D < 15, !.
dist(D, ['veinte.ogg']) :-  D < 25, !.
dist(D, ['treinta.ogg']) :-  D < 35, !.
dist(D, ['cuarenta.ogg']) :-  D < 45, !.
dist(D, ['cincuenta.ogg']) :-  D < 55, !.
dist(D, ['sesenta.ogg']) :-  D < 65, !.
dist(D, ['setenta.ogg']) :-  D < 75, !.
dist(D, ['ochenta.ogg']) :-  D < 85, !.
dist(D, ['noventa.ogg']) :-  D < 95, !.
dist(D, ['cien.ogg']) :-  D < 125, !.
dist(D, ['ciento.ogg', 'cincuenta.ogg']) :-  D < 175, !.
dist(D, ['dos.ogg','cientos.ogg']) :-  D < 225, !.
dist(D, ['dos.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 275, !.
dist(D, ['tres.ogg','cientos.ogg']) :-  D < 325, !.
dist(D, ['tres.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 375, !.
dist(D, ['cuatro.ogg','cientos.ogg']) :-  D < 425, !.
dist(D, ['cuatro.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 475, !.
dist(D, ['quinientos.ogg']) :-  D < 525, !.
dist(D, ['quinientos.ogg', 'cincuenta.ogg']) :-  D < 575, !.
dist(D, ['seis.ogg','cientos.ogg']) :-  D < 625, !.
dist(D, ['seis.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 675, !.
dist(D, ['setescientos.ogg']) :-  D < 725, !.
dist(D, ['setescientos.ogg', 'cincuenta.ogg']) :-  D < 775, !.
dist(D, ['ocho.ogg','cientos.ogg']) :-  D < 825, !.
dist(D, ['ocho.ogg','cientos.ogg', 'cincuenta.ogg']) :-  D < 875, !.
dist(D, ['novecientos.ogg']) :-  D < 925, !.
dist(D, ['novecientos.ogg', 'cincuenta.ogg']) :-  !.


distance(Dist) == ['cerca.ogg', 'un.ogg', 'kilometro.ogg'] :- Dist < 1500.
distance(Dist) == ['cerca.ogg', 'dos.ogg', 'kilometros.ogg'] :- Dist < 2500.
distance(Dist) == ['cerca.ogg', 'tres.ogg', 'kilometros.ogg'] :- Dist < 3500.
distance(Dist) == ['cerca.ogg', 'cuatro.ogg', 'kilometros.ogg'] :- Dist < 4500.
distance(Dist) == ['cerca.ogg', 'cinco.ogg', 'kilometros.ogg'] :- Dist < 5500.
distance(Dist) == ['cerca.ogg', 'seis.ogg', 'kilometros.ogg'] :- Dist < 6500.
distance(Dist) == ['cerca.ogg', 'siete.ogg', 'kilometros.ogg'] :- Dist < 7500.
distance(Dist) == ['cerca.ogg', 'ocho.ogg', 'kilometros.ogg'] :- Dist < 8500.
distance(Dist) == ['cerca.ogg', 'nueve.ogg', 'kilometros.ogg'] :- Dist < 9500.
distance(Dist) == ['cerca.ogg', X, 'kilometros.ogg'] :- D is Dist/1000, dist(D, X).


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