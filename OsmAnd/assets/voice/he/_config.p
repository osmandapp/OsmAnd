:- op('==', xfy, 500).
version(0).

%Provided by VnMedia VnrMedia@gmail.com
%Speaked by Shai Ben-yaakov
% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['turn_left-e.ogg']).
turn('left_sh', ['turn_sharp_left-e.ogg']).
turn('left_sl', ['turn_slight_left-e.ogg']).
turn('right', ['turn_right-e.ogg']).
turn('right_sh', ['turn_sharp_right-e.ogg']).
turn('right_sl', ['turn_slight_right-e.ogg']).
turn('right_keep', ['keep_right-e.ogg']).
turn('left_keep', ['keep_left-e.ogg']).

prepare_turn(Turn, Dist) == ['Prepare_to-a.ogg', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['after.ogg', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['Prepare_to-a.ogg', D,'turn_back-e.ogg'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['prepare_to-enter.ogg', 'after.ogg', D] :- 
		distance(Dist) == D.

make_ut(Dist) == ['after.ogg', D, 'turn_back-e.ogg'] :- 
			distance(Dist) == D.
make_ut == ['turn_back-e.ogg'].

make_ut_wp == ['when_possible_please_make_a_u_turn.ogg'].




roundabout(Dist, _Angle, Exit) == ['after.ogg', D, 'enter_the_roundabout-e.ogg', 'and_take.ogg', 'exit-e.ogg', E] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['taking.ogg', 'exit-e.ogg', E] :- nth(Exit, E).

and_arrive_destination == ['arrive_at_your_destination-e.ogg']. % Miss and?
reached_destination == ['you_have_reached_your_destination.ogg'].
and_arrive_intermediate == ['arrive_at_viapoint-e.ogg'].
reached_intermediate == ['you_have_reached_a_viapoint.ogg'].

then == ['then.ogg'].

bear_right == ['keep_right-e.ogg'].
bear_left == ['keep_left-e.ogg'].
route_recalc(Dist) == ['recalc.ogg', D]:- distance(Dist) == D.   %nothing to said possibly beep?	
route_new_calc(Dist) == ['the_trip_is_about.ogg', D] :- distance(Dist) == D. % nothing to said possibly beep?

location_lost == ['gps_signal_lost.ogg'].

go_ahead(Dist) == ['Follow-the-road-for.ogg',  D]:- distance(Dist) == D.
go_ahead == ['continue_straight-e.ogg'].

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
nth(10, '10th.ogg').
nth(11, '11th.ogg').
nth(12, '12th.ogg').
nth(13, '13th.ogg').
nth(14, '14th.ogg').
nth(15, '15th.ogg').
nth(16, '16th.ogg').
nth(17, '17th.ogg').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'meters-e.ogg',T).
dist(D, ['20.ogg']) :-  D < 30, !.
dist(D, ['30.ogg']) :-  D < 40, !.
dist(D, ['40.ogg']) :-  D < 50, !.
dist(D, ['50.ogg']) :-  D < 60, !.
dist(D, ['60.ogg']) :-  D < 70, !.
dist(D, ['70.ogg']) :-  D < 80, !.
dist(D, ['80.ogg']) :-  D < 90, !.
dist(D, ['90.ogg']) :-  D < 100, !.
dist(D, ['100.ogg']) :-  D < 110, !.
dist(D, ['100.ogg','and_10.ogg']) :-  D < 120, !. 
dist(D, ['100.ogg', S]) :-  D < 200, T is D - 100, dist(T, [S]), !.
dist(D, ['200.ogg']) :-  D < 210, !.
dist(D, ['200.ogg','and_10.ogg']) :-  D < 220, !. 
dist(D, ['200.ogg', S]) :-  D < 300, T is D - 200, dist(T, [S]), !.
dist(D, ['300.ogg']) :-  D < 310, !.
dist(D, ['300.ogg','and_10.ogg']) :-  D < 320, !. 
dist(D, ['300.ogg', S]) :-  D < 400, T is D - 300, dist(T, [S]), !.

dist(D, ['400.ogg']) :-  D < 410, !.
dist(D, ['400.ogg','and_10.ogg']) :-  D < 420, !. 
dist(D, ['400.ogg', S]) :-  D < 500, T is D - 400, dist(T, [S]), !.
dist(D, ['500.ogg']) :-  D < 510, !.
dist(D, ['500.ogg','and_10.ogg']) :-  D < 520, !. 
dist(D, ['500.ogg', S]) :-  D < 600, T is D - 500, dist(T, [S]), !.
dist(D, ['600.ogg']) :-  D < 610, !.
dist(D, ['600.ogg','and_10.ogg']) :-  D < 620, !. 
dist(D, ['600.ogg', S]) :-  D < 700, T is D - 600, dist(T, [S]), !.
dist(D, ['700.ogg']) :-  D < 710, !.
dist(D, ['700.ogg','and_10.ogg']) :-  D < 720, !. 
dist(D, ['700.ogg', S]) :-  D < 800, T is D - 700, dist(T, [S]), !.
dist(D, ['800.ogg']) :-  D < 810, !.
dist(D, ['800.ogg','and_10.ogg']) :-  D < 820, !. 
dist(D, ['800.ogg', S]) :-  D < 900, T is D - 800, dist(T, [S]), !.
dist(D, ['900.ogg']) :-  D < 910, !.
dist(D, ['900.ogg','and_10.ogg']) :-  D < 920, !. 
dist(D, ['900.ogg', S]) :-  D < 1000, T is D - 900, dist(T, [S]), !.

distance(Dist) == ['more_than.ogg', '1.ogg', 'kilometer-e.ogg'] :- Dist < 1500.
distance(Dist) == ['more_than.ogg', '2.ogg', 'kilometers-e.ogg'] :- Dist < 3000.
distance(Dist) == ['more_than.ogg', '3.ogg', 'kilometers-e.ogg'] :- Dist < 4000.
distance(Dist) == ['more_than.ogg', '4.ogg', 'kilometers-e.ogg'] :- Dist < 5000.
distance(Dist) == ['more_than.ogg', '5.ogg', 'kilometers-e.ogg'] :- Dist < 6000.
distance(Dist) == ['more_than.ogg', '6.ogg', 'kilometers-e.ogg'] :- Dist < 7000.
distance(Dist) == ['more_than.ogg', '7.ogg', 'kilometers-e.ogg'] :- Dist < 8000.
distance(Dist) == ['more_than.ogg', '8.ogg', 'kilometers-e.ogg'] :- Dist < 9000.
distance(Dist) == ['more_than.ogg', '9.ogg', 'kilometers-e.ogg'] :- Dist < 10000.
distance(Dist) == ['more_than.ogg', '10.ogg', 'kilometers-e.ogg'] :- Dist < 11000.
distance(Dist) == ['more_than.ogg', '11.ogg', 'kilometers-e.ogg'] :- Dist < 12000.
distance(Dist) == ['more_than.ogg', '12.ogg', 'kilometers-e.ogg'] :- Dist < 13000.
distance(Dist) == ['more_than.ogg', '13.ogg', 'kilometers-e.ogg'] :- Dist < 14000.
distance(Dist) == ['more_than.ogg', '14.ogg', 'kilometers-e.ogg'] :- Dist < 15000.
distance(Dist) == ['more_than.ogg', '15.ogg', 'kilometers-e.ogg'] :- Dist < 16000.
distance(Dist) == ['more_than.ogg', '16.ogg', 'kilometers-e.ogg'] :- Dist < 17000.
distance(Dist) == ['more_than.ogg', '17.ogg', 'kilometers-e.ogg'] :- Dist < 18000.
distance(Dist) == ['more_than.ogg', '18.ogg', 'kilometers-e.ogg'] :- Dist < 19000.
distance(Dist) == ['more_than.ogg', '19.ogg', 'kilometers-e.ogg'] 
:- Dist < 20000.

distance(Dist) == ['more_than.ogg', X, 'kilometers-e.ogg'] :- D is Dist/1000, dist(D, X).



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
