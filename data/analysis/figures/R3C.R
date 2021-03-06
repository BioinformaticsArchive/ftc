# Plot of sim(moa) vs. sim(structure)
# Only compounds that are present in different categories (based on a treshold) are
# represented.

values <- read.csv("/home/samuel/git/ftc/data/analysis/diff_cats_1lvl.csv", 
                   head=TRUE, 
                   sep=","
)

moa <- values$firstSim
struc <- values$secondSim
color <- '#FF0700'

# FF0700 FFD500 3B14AF 00C90D

plot(struc, 
     moa,
     xlim=c(0,1),
     ylim=c(0,1),
     pch='*',
     main='Structural similarity versus mode of action similarity\nOnly compounds present in the different ATC categories', 
     ylab="Mode of action similarity (Jaccard index over ancestor classes)",
     xlab="Structural similarity (Tanimoto coeff. over hybridization fingerprint)",
     col=color
)
abline(h=mean(moa), v=mean(struc), col=color, lwd=3)

par(new=TRUE)