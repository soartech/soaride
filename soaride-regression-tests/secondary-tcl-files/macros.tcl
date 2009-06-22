

proc lhs {} {
    return "(state <s> ^superstatex nil ^io.input-link <il>)"
}

proc lhs-with-error {} {
    return "(statex <s> ^superstate nil)"
}
