:- op('==', xfy, 500).
version(0).


% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['turn.ogg', delay_350, 'left.ogg']).
turn('left_sh', ['turn_sharply.ogg', delay_350, 'left.ogg']).
turn('left_sl', ['turn_slightly_left.ogg']).
turn('right', ['turn.ogg', delay_350, 'right.ogg']).
turn('right_sh', ['turn_sharply.ogg', delay_350,'right.ogg']).
turn('right_sl', ['turn_slightly_right.ogg']).

prepare_turn(Turn, Dist) == ['Prepare_to.ogg', 'in.ogg', delay_300, D, delay_300, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['in.ogg', delay_250, D, delay_250, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['Prepare_to.ogg', 'in.ogg', delay_300, D, delay_300,'Turn_back.ogg'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['Prepare_to.ogg', 'in.ogg', delay_300, D, delay_300, 'roundabout.ogg'] :- 
		distance(Dist) == D.

make_ut(Dist) == ['in.ogg', delay_300, D, delay_300, 'Turn_back.ogg'] :- 
			distance(Dist) == D.
make_ut == ['Turn_back.ogg'].

roundabout(Dist, _Angle, Exit) == ['in.ogg', delay_300, D, delay_300, 'roundabout.ogg', delay_250, 'DO.ogg', delay_250, E, 'the_exit.ogg'] :- 
			distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['DO.ogg', delay_250,  E, 'the_exit.ogg'] :- nth(Exit, E).

and_arrive_destination == ['arrive_at_destination.ogg'].
then == ['then.ogg', delay_350].
reached_destination == ['you_reached.ogg',delay_250, 'TO_DESTINATION.ogg'].
bear_right == ['bear_right.ogg'].
bear_left == ['bear_left.ogg'].
route_recalc(_Dist) == ['recalc.ogg'].

go_ahead(Dist) == ['Drive.ogg', delay_250,  D]:- distance(Dist) == D.
go_ahead == ['continue.ogg', 'stright.ogg'].

%% 
nth(1, '1st.ogg').
nth(2, '2nd.ogg').
nth(3, '3rd.ogg').
nth(4, '4th.ogg').
nth(5, '5th.ogg').
nth(6, '6th.ogg').
nth(7, '7th.ogg').
nth(8, '8th.ogg').
nth(9, '9th.ogg').

%%% distance measure
distance(Dist) == [F, 'meters.ogg'] :- Dist < 1000, dist(Dist, F).
dist(D, '10.ogg') :-  D < 20, !.
dist(D, '20.ogg') :-  D < 30, !.
dist(D, '30.ogg') :-  D < 40, !.
dist(D, '40.ogg') :-  D < 50, !.
dist(D, '50.ogg') :-  D < 60, !.
dist(D, '60.ogg') :-  D < 70, !.
dist(D, '70.ogg') :-  D < 80, !.
dist(D, '80.ogg') :-  D < 90, !.
dist(D, '90.ogg') :-  D < 100, !.
dist(D, '100.ogg') :-  D < 150, !.
dist(D, '150.ogg') :-  D < 200, !.
dist(D, '200.ogg') :-  D < 250, !.
dist(D, '250.ogg') :-  D < 300, !.
dist(D, '300.ogg') :-  D < 350, !.
dist(D, '350.ogg') :-  D < 400, !.
dist(D, '400.ogg') :-  D < 450, !.
dist(D, '450.ogg') :-  D < 500, !.
dist(D, '500.ogg') :-  D < 550, !.
dist(D, '550.ogg') :-  D < 600, !.
dist(D, '600.ogg') :-  D < 650, !.
dist(D, '650.ogg') :-  D < 700, !.
dist(D, '700.ogg') :-  D < 750, !.
dist(D, '750.ogg') :-  D < 800, !.
dist(D, '800.ogg') :-  D < 850, !.
dist(D, '850.ogg') :-  D < 900, !.
dist(D, '900.ogg') :-  D < 950, !.
dist(D, '950.ogg') :-  !.


distance(Dist) == ['more_than.ogg', '1.ogg', 'kilometr.ogg'] :- Dist < 1500.
distance(Dist) == ['more_than.ogg', '1.5k.ogg', 'kilometra.ogg'] :- Dist < 2000.
distance(Dist) == ['more_than.ogg', '2.ogg', 'kilometra.ogg'] :- Dist < 3000.
distance(Dist) == ['more_than.ogg', '3.ogg', 'kilometra.ogg'] :- Dist < 4000.
distance(Dist) == ['more_than.ogg', '4.ogg', 'kilometra.ogg'] :- Dist < 5000.
distance(Dist) == ['more_than.ogg', '5.ogg', 'kilometrov.ogg'] :- Dist < 6000.
distance(Dist) == ['more_than.ogg', '6.ogg', 'kilometrov.ogg'] :- Dist < 7000.
distance(Dist) == ['more_than.ogg', '7.ogg', 'kilometrov.ogg'] :- Dist < 8000.
distance(Dist) == ['more_than.ogg', '8.ogg', 'kilometrov.ogg'] :- Dist < 9000.
distance(Dist) == ['more_than.ogg', '9.ogg', 'kilometrov.ogg'] :- Dist < 10000.
distance(Dist) == ['more_than.ogg', X, 'kilometrov.ogg'] :- D is Dist/1000, dist(D, X).



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