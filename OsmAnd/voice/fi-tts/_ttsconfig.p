:- op('==', xfy, 500).
version(100).
language(fi).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['käänny vasemmalle ']).
turn('left_sh', ['käänny jyrkästi vasemmalle ']).
turn('left_sl', ['käänny loivasti vasemmalle ']).
turn('right', ['käänny oikealle ']).
turn('right_sh', ['käänny jyrkästi oikealle ']).
turn('right_sl', ['käänny loivasti oikealle ']).

prepturn('left', ['kääntymään vasemmalle ']).
prepturn('left_sh', ['kääntymään jyrkästi vasemmalle ']).
prepturn('left_sl', ['kääntymään loivasti vasemmalle ']).
prepturn('right', ['kääntymään oikealle ']).
prepturn('right_sh', ['kääntymään jyrkästi oikealle ']).
prepturn('right_sl', ['kääntymään loivasti oikealle ']).

prepare_turn(Turn, Dist) == ['Valmistaudu ', D, ' päästä ', M] :- 
			distance(Dist, metrin) == D, prepturn(Turn, M).
turn(Turn, Dist) == [D, ' päästä ', M] :- 
			distance(Dist, metrin) == D, turn(Turn, M).
turn(Turn) == ['Nyt, ', M] :- turn(Turn, M).


prepare_make_ut(Dist) == ['Valmistaudu kääntymään takaisin ', D, ' päästä'] :- 
		distance(Dist, metrin) == D.

prepare_roundabout(Dist) == ['Valmistaudu ajamaan liikenneympyrään ', D, ' päästä'] :- 
		distance(Dist, metrin) == D.

make_ut(Dist) == ['Käänny takaisin ', D, ' päästä '] :- 
			distance(Dist, metrin) == D.
make_ut == ['Nyt, käänny takaisin '].

roundabout(Dist, _Angle, Exit) == ['Aja liikenneympyrään ', D, ' päästä ja ota ', E, ' liittymä'] :- distance(Dist, metrin) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['Nyt, ota ', E, ' liittymä'] :- nth(Exit, E).

and_arrive_destination == ['ja olet perillä ']. % Miss and?
then == ['sitten '].
reached_destination == ['olet perillä '].
bear_right == ['pidä oikea '].
bear_left == ['pidä vasen '].
route_recalc(Dist) == ['Reitin uudelleenlaskenta '].  %nothing to said possibly beep?	
route_new_calc(Dist) == ['Matkan pituus on ', D] :- distance(Dist, metria) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['Jatka suoraan ', D]:- distance(Dist, metria) == D.
go_ahead == ['Jatka suoraan '].

%% 
nth(1, 'ensimmäinen ').
nth(2, 'toinen ').
nth(3, 'kolmas ').
nth(4, 'neljäs ').
nth(5, 'viides ').
nth(6, 'kuudes ').
nth(7, 'seitsemäs ').
nth(8, 'kahdeksas ').
nth(9, 'yhdeksäs ').
nth(10, 'kymmenes ').
nth(11, 'yhdestoista ').
nth(12, 'kahdestoista ').
nth(13, 'kolmastoista ').
nth(14, 'neljästoista ').
nth(15, 'viidestoista ').
nth(16, 'kuudestoista ').
nth(17, 'seitsemästoista ').


%%% distance measure
distance(Dist, metrin) == T :- Dist < 1000, dist(Dist, F), append(F, ' metrin',T).
distance(Dist, metria) == T :- Dist < 1000, dist(Dist, F), append(F, ' metriä',T).
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

distkm(D, ['10 ']) :-  D < 10.5, !.
distkm(D, ['11 ']) :-  D < 11.5, !.
distkm(D, ['12 ']) :-  D < 12.5, !.
distkm(D, ['13 ']) :-  D < 13.5, !.
distkm(D, ['14 ']) :-  D < 14.5, !.
distkm(D, ['15 ']) :-  D < 15.5, !.
distkm(D, ['16 ']) :-  D < 16.5, !.
distkm(D, ['17 ']) :-  D < 17.5, !.
distkm(D, ['18 ']) :-  D < 18.5, !.
distkm(D, ['19 ']) :-  D < 19.5, !.
distkm(D, ['20 ']) :-  D < 20.5, !.
distkm(D, ['21 ']) :-  D < 21.5, !.
distkm(D, ['22 ']) :-  D < 22.5, !.
distkm(D, ['23 ']) :-  D < 23.5, !.
distkm(D, ['24 ']) :-  D < 24.5, !.
distkm(D, ['25 ']) :-  D < 25.5, !.
distkm(D, ['26 ']) :-  D < 26.5, !.
distkm(D, ['27 ']) :-  D < 27.5, !.
distkm(D, ['28 ']) :-  D < 28.5, !.
distkm(D, ['29 ']) :-  D < 29.5, !.
distkm(D, ['30 ']) :-  D < 35, !.
distkm(D, ['40 ']) :-  D < 45, !.
distkm(D, ['50 ']) :-  D < 55, !.
distkm(D, ['60 ']) :-  D < 65, !.
distkm(D, ['70 ']) :-  D < 75, !.
distkm(D, ['80 ']) :-  D < 85, !.
distkm(D, ['90 ']) :-  D < 95, !.
distkm(D, ['100 ']) :-  D < 125, !.
distkm(D, ['150 ']) :-  D < 175, !.
distkm(D, ['200 ']) :-  D < 225, !.
distkm(D, ['250 ']) :-  D < 275, !.
distkm(D, ['300 ']) :-  D < 325, !.
distkm(D, ['350 ']) :-  D < 375, !.
distkm(D, ['400 ']) :-  D < 425, !.
distkm(D, ['450 ']) :-  D < 475, !.
distkm(D, ['500 ']) :-  D < 525, !.
distkm(D, ['550 ']) :-  D < 575, !.
distkm(D, ['600 ']) :-  D < 625, !.
distkm(D, ['650 ']) :-  D < 675, !.
distkm(D, ['700 ']) :-  D < 725, !.
distkm(D, ['750 ']) :-  D < 775, !.
distkm(D, ['800 ']) :-  D < 825, !.
distkm(D, ['850 ']) :-  D < 875, !.
distkm(D, ['900 ']) :-  D < 925, !.
distkm(D, ['950 ']) :-  D < 975, !.
distkm(D, ['1000 ']) :-  !.

distance(Dist, metrin) == T :- Dist < 1000, dist(Dist, F), append(F, ' metrin',T).
distance(Dist, metria) == T :- Dist < 1000, dist(Dist, F), append(F, ' metriä',T).

distance(Dist, metrin) == ['noin 1 kilometrin '] :- Dist < 1500.
distance(Dist, metrin) == ['noin 2 kilometerin '] :- Dist < 2500.
distance(Dist, metrin) == ['noin 3 kilometerin '] :- Dist < 3500.
distance(Dist, metrin) == ['noin 4 kilometerin '] :- Dist < 4500.
distance(Dist, metrin) == ['noin 5 kilometerin '] :- Dist < 5500.
distance(Dist, metrin) == ['noin 6 kilometerin '] :- Dist < 6500.
distance(Dist, metrin) == ['noin 7 kilometerin '] :- Dist < 7500.
distance(Dist, metrin) == ['noin 8 kilometerin '] :- Dist < 8500.
distance(Dist, metrin) == ['noin 9 kilometerin '] :- Dist < 9500.
% Note: do not put space after word "noin" because for some reason the SVOX Finnish Satu Voice announces the number wrong if there is a space
distance(Dist, metrin) == ['noin', X, ' kilometerin '] :- D is Dist/1000, distkm(D, X).

distance(Dist, metria) == ['noin 1 kilometri '] :- Dist < 1500.
distance(Dist, metria) == ['noin 2 kilometeriä '] :- Dist < 2500.
distance(Dist, metria) == ['noin 3 kilometeriä '] :- Dist < 3500.
distance(Dist, metria) == ['noin 4 kilometeriä '] :- Dist < 4500.
distance(Dist, metria) == ['noin 5 kilometeriä '] :- Dist < 5500.
distance(Dist, metria) == ['noin 6 kilometeriä '] :- Dist < 6500.
distance(Dist, metria) == ['noin 7 kilometeriä '] :- Dist < 7500.
distance(Dist, metria) == ['noin 8 kilometeriä '] :- Dist < 8500.
distance(Dist, metria) == ['noin 9 kilometeriä '] :- Dist < 9500.
% Note: do not put space after word "noin" because for some reason the SVOX Finnish Satu Voice announces the number wrong if there is a space
distance(Dist, metria) == ['noin', X, ' kilometriä '] :- D is Dist/1000, distkm(D, X).


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