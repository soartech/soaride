

proc lhs {} {
  set no_error "nil"
  return "(state <s> ^superstate $no_error)"
}

proc lhs-with-error {} {
    return [recurse]
}
proc recurse {} {
    return "(state <s> ^superstate $error)"
}