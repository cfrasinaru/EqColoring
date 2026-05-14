setwd("d:/java/EqColoring/")
paperDir <- "d:/articole/!EquitableColoring/"
dataFile1 <- "random__d=0.9_k=5_p=10-100"
dataFile2 <- "random__d=0.9_k=3_p=10-100"
dataFile3 <- "random__d=0.7_k=3_p=10-100"
dataFile4 <- "random__d=0.5_k=3_p=10-100"

source("R/plot_results.R")

setEPS()
postscript(paste(paperDir, "plots/AssignmentVsSetCovering.eps", sep=""), width=7, height=4)
par(mfrow = c(2, 2), mar = c(2, 2, 1, 0.5))
plot_results(dataFile1, title="k=5, d=0.9", xlab="")
plot_results(dataFile2, title="k=3, d=0.9", xlab="", ylab="")
plot_results(dataFile3, title="k=3, d=0.7")
plot_results(dataFile4, title="k=3, d=0.5", ylab="")
dev.off()

pdf(paste(paperDir, "plots/AssignmentVsSetCovering.pdf", sep=""), width=7, height=4)
par(mfrow = c(2, 2), mar = c(2, 2, 1, 0.5))
plot_results(dataFile1, title="k=5, d=0.9", xlab="")
plot_results(dataFile2, title="k=3, d=0.9", xlab="", ylab="")
plot_results(dataFile3, title="k=3, d=0.7")
plot_results(dataFile4, title="k=3, d=0.5", ylab="")
dev.off()