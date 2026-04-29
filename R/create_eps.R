create_eps <- function(file, xlab = "Number of vertices", ylim1 = NULL, ylim2 = NULL) {

#paperDir <- "d:/articole/!graph4j/"
paperDir <- "d:/articole/!EquitableColoring/"

setEPS()
postscript(paste(paperDir, "eps/",file,".eps",sep=""), width=7, height=3.5)
par(mfrow=c(1,2))
plot_results(file, xlab, ylim1)
plot_results(file, xlab, ylim2)
dev.off()

}