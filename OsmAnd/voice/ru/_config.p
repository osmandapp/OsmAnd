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

prepare_turn(Turn, Dist) == ['Prepare_to.ogg', 'in.ogg', delay_300, D, delay_300, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['in.ogg', delay_250, D, delay_250, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Prepare_to.ogg', 'in.ogg', delay_300, D, delay_300,'Turn_back.ogg'] :- distance(Dist) == D.
make_ut(Dist) == ['in.ogg', delay_300, D, delay_300, 'Turn_back.ogg'] :- distance(Dist) == D.
make_ut == ['Turn_back.ogg'].
make_ut_wp == ['Turn_back.ogg'].

prepare_roundabout(Dist) == ['Prepare_to.ogg', 'in.ogg', delay_300, D, delay_300, 'roundabout.ogg'] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['in.ogg', delay_300, D, delay_300, 'roundabout.ogg', delay_250, 'DO.ogg', delay_250, E, 'the_exit.ogg'] :- 
	distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['DO.ogg', delay_250,  E, 'the_exit.ogg'] :- nth(Exit, E).

go_ahead == ['continue.ogg', 'stright.ogg'].
go_ahead(Dist) == ['Drive.ogg', delay_250,  D]:- distance(Dist) == D.

and_arrive_destination == ['arrive_at_destination.ogg'].

then == ['then.ogg', delay_350].
reached_destination == ['you_reached.ogg',delay_250, 'TO_DESTINATION.ogg'].
bear_right == ['bear_right.ogg'].
bear_left == ['bear_left.ogg'].

route_new_calc(Dist) == ['have_a_nice_trip_drive_carefully.ogg', delay_150, D] :- distance(Dist) == D. 
route_recalc(Dist) == ['recalc.ogg', delay_150, D]:- distance(Dist) == D.

location_lost == ['gps_signal_lost.ogg'].


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
dist(D, '10.ogg') :-  D < 15, !.
dist(D, '20.ogg') :-  D < 25, !.
dist(D, '30.ogg') :-  D < 35, !.
dist(D, '40.ogg') :-  D < 45, !.
dist(D, '50.ogg') :-  D < 55, !.
dist(D, '60.ogg') :-  D < 65, !.
dist(D, '70.ogg') :-  D < 75, !.
dist(D, '80.ogg') :-  D < 85, !.
dist(D, '90.ogg') :-  D < 95, !.
dist(D, '100.ogg') :-  D < 125, !.
dist(D, '150.ogg') :-  D < 175, !.
dist(D, '200.ogg') :-  D < 225, !.
dist(D, '250.ogg') :-  D < 275, !.
dist(D, '300.ogg') :-  D < 325, !.
dist(D, '350.ogg') :-  D < 375, !.
dist(D, '400.ogg') :-  D < 425, !.
dist(D, '450.ogg') :-  D < 475, !.
dist(D, '500.ogg') :-  D < 525, !.
dist(D, '550.ogg') :-  D < 575, !.
dist(D, '600.ogg') :-  D < 625, !.
dist(D, '650.ogg') :-  D < 675, !.
dist(D, '700.ogg') :-  D < 725, !.
dist(D, '750.ogg') :-  D < 775, !.
dist(D, '800.ogg') :-  D < 825, !.
dist(D, '850.ogg') :-  D < 875, !.
dist(D, '900.ogg') :-  D < 925, !.
dist(D, '950.ogg') :-  !.


distance(Dist) == ['about.ogg', '1.ogg', 'kilometr.ogg'] :- Dist < 1500.
distance(Dist) == ['about.ogg', '2.ogg', 'kilometra.ogg'] :- Dist < 2500.
distance(Dist) == ['about.ogg', '3.ogg', 'kilometra.ogg'] :- Dist < 3500.
distance(Dist) == ['about.ogg', '4.ogg', 'kilometra.ogg'] :- Dist < 4500.
distance(Dist) == ['about.ogg', '5.ogg', 'kilometrov.ogg'] :- Dist < 5500.
distance(Dist) == ['about.ogg', '6.ogg', 'kilometrov.ogg'] :- Dist < 6500.
distance(Dist) == ['about.ogg', '7.ogg', 'kilometrov.ogg'] :- Dist < 7500.
distance(Dist) == ['about.ogg', '8.ogg', 'kilometrov.ogg'] :- Dist < 8500.
distance(Dist) == ['about.ogg', '9.ogg', 'kilometrov.ogg'] :- Dist < 9500.
distance(Dist) == ['about.ogg', X, 'kilometrov.ogg'] :- D is Dist/1000, dist(D, X).


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