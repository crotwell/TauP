#include "taupnative.h"

main() {
   TauPStruct taup;
	int result;
	int numArrivals;
	int i;

	result = TauPInit(&taup, "prem");
	if (result != 0) {
		fprintf(stderr, "Error initializing JVM or TauP_Time.\n");
		exit(1);
	}

	result = TauPAppendPhases(taup, "P,S,PcP,ScS");
	printf("TauPAppendPhases result=%d\n", result);
	if (result != 0) {exit(result);}

	result = TauPSetDepth(taup, 100.0);
	printf("TauPSetDepth result=%d\n", result);
	if (result != 0) {exit(result);}

	result = TauPCalculate(taup, 60.0);
	printf("TauPCalculate result=%d\n", result);
	if (result != 0) {exit(result);}

	numArrivals = TauPGetNumArrivals(taup);
	printf("There are %d arrivals\n",numArrivals);
	printf("-----------------------------------------------\n");
	for (i=0; i< numArrivals; i++) {
		printf("%s  %f\n",TauPGetArrivalName(taup, i), TauPGetArrivalTime(taup, i));
	}

	result = TauPClearPhases(taup);
	printf("TauPClearPhases result=%d\n", result);
	if (result != 0) {exit(result);}

	result = TauPAppendPhases(taup, "PKP,SKP,PKIKP,SKIKS,PKKP,SKKS");
	printf("TauPAppendPhases result=%d\n", result);
	if (result != 0) {exit(result);}

	result = TauPSetDepth(taup, 100.0);
	printf("TauPSetDepth result=%d\n", result);
	if (result != 0) {exit(result);}

	result = TauPCalculate(taup, 160.0);
	printf("TauPCalculate result=%d\n", result);
	if (result != 0) {exit(result);}

	numArrivals = TauPGetNumArrivals(taup);
	printf("There are %d arrivals\n",numArrivals);
	printf("------------------------------------------------------\n");
	for (i=0; i< numArrivals; i++) {
		printf("%10s  %8.3f %8.3f %8.3f %8.3f  %s\n",
			TauPGetArrivalName(taup, i), 
			TauPGetArrivalTime(taup, i),
			(TauPGetArrivalDist(taup, i)*180/3.141592),
			(TauPGetArrivalRayParam(taup, i)*3.141592/180),
			TauPGetArrivalSourceDepth(taup, i),
			TauPGetArrivalPuristName(taup, i));
	}

	return TauPDestroy(taup);
}


