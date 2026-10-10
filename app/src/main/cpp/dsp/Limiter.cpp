#include "Limiter.h"
#include <cmath>
#include <algorithm>

void Limiter::setCeiling(float ceilingDbfs) {
    mCeiling.store(std::pow(10.f, ceilingDbfs / 20.f));
}

float Limiter::process(float sample) {
    float ceiling = mCeiling.load();
    return std::max(-ceiling, std::min(ceiling, sample));
}
